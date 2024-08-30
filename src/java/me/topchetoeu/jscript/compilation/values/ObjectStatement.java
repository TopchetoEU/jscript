package me.topchetoeu.jscript.compilation.values;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Statement;

public class ObjectStatement extends Statement {
    public static class ObjProp {
        public final String name;
        public final String access;
        public final FunctionStatement func;
    
        public ObjProp(String name, String access, FunctionStatement func) {
            this.name = name;
            this.access = access;
            this.func = func;
        }
    }

    public final Map<String, Statement> map;
    public final Map<String, FunctionStatement> getters;
    public final Map<String, FunctionStatement> setters;

    @Override public boolean pure() {
        for (var el : map.values()) {
            if (!el.pure()) return false;
        }

        return true;
    }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.loadObj());

        for (var el : map.entrySet()) {
            target.add(Instruction.dup());
            target.add(Instruction.pushValue(el.getKey()));
            var val = el.getValue();
            FunctionStatement.compileWithName(val, target, true, el.getKey().toString());
            target.add(Instruction.storeMember());
        }

        var keys = new ArrayList<Object>();
        keys.addAll(getters.keySet());
        keys.addAll(setters.keySet());

        for (var key : keys) {
            target.add(Instruction.pushValue((String)key));

            if (getters.containsKey(key)) getters.get(key).compile(target, true);
            else target.add(Instruction.pushUndefined());

            if (setters.containsKey(key)) setters.get(key).compile(target, true);
            else target.add(Instruction.pushUndefined());

            target.add(Instruction.defProp());
        }

        if (!pollute) target.add(Instruction.discard());
    }

    public ObjectStatement(Location loc, Map<String, Statement> map, Map<String, FunctionStatement> getters, Map<String, FunctionStatement> setters) {
        super(loc);
        this.map = map;
        this.getters = getters;
        this.setters = setters;
    }

    private static ParseRes<String> parsePropName(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        var res = ParseRes.first(src, i + n,
            Parsing::parseIdentifier,
            Parsing::parseString,
            (s, j) -> Parsing.parseNumber(s, j, false)
        );
        n += res.n;

        if (!res.isSuccess()) return res.chainError();
        return ParseRes.res(res.result.toString(), n);
    }
    private static ParseRes<ObjectStatement.ObjProp> parseObjectProp(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var access = Parsing.parseIdentifier(src, i + n);
        if (!access.isSuccess()) return ParseRes.failed();
        if (!access.result.equals("get") && !access.result.equals("set")) return ParseRes.failed();
        n += access.n;

        var name = parsePropName(src, i + n);
        if (!name.isSuccess()) return name.chainError(src.loc(i + n), "Expected a property name after '" + access + "'");
        n += name.n;

        var params = JavaScript.parseParamList(src, i + n);
        if (!params.isSuccess()) return params.chainError(src.loc(i + n), "Expected an argument list");
        n += params.n;

        var body = CompoundStatement.parse(src, i + n);
        if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected a compound statement for property accessor.");
        n += body.n;

        var end = src.loc(i + n - 1);

        return ParseRes.res(new ObjProp(
            name.result, access.result,
            new FunctionStatement(loc, end, access + " " + name.result.toString(), params.result.toArray(String[]::new), false, body.result)
        ), n);
    }

    public static ParseRes<ObjectStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "{")) return ParseRes.failed();
        n++;
        n += Parsing.skipEmpty(src, i + n);

        var values = new LinkedHashMap<String, Statement>();
        var getters = new LinkedHashMap<String, FunctionStatement>();
        var setters = new LinkedHashMap<String, FunctionStatement>();

        if (src.is(i + n, "}")) {
            n++;
            return ParseRes.res(new ObjectStatement(loc, values, getters, setters), n);
        }

        while (true) {
            var prop = parseObjectProp(src, i + n);

            if (prop.isSuccess()) {
                n += prop.n;

                if (prop.result.access.equals("set")) setters.put(prop.result.name, prop.result.func);
                else getters.put(prop.result.name, prop.result.func);
            }
            else {
                var name = parsePropName(src, i + n);
                if (!name.isSuccess()) return prop.chainError(src.loc(i + n), "Expected a field name");
                n += name.n;
                n += Parsing.skipEmpty(src, i + n);

                if (!src.is(i + n, ":")) return ParseRes.error(src.loc(i + n), "Expected a colon");
                n++;

                var valRes = JavaScript.parseExpression(src, i + n, 2);
                if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value in array list");
                n += valRes.n;

                values.put(name.result, valRes.result);
            }

            n += Parsing.skipEmpty(src, i + n);
            if (src.is(i + n, ",")) {
                n++;
                n += Parsing.skipEmpty(src, i + n);

                if (src.is(i + n, "}")) {
                    n++;
                    break;
                }

                continue;
            }
            else if (src.is(i + n, "}")) {
                n++;
                break;
            }
            else ParseRes.error(src.loc(i + n), "Expected a comma or a closing brace.");
        }
    
        return ParseRes.res(new ObjectStatement(loc, values, getters, setters), n);
    }

}
