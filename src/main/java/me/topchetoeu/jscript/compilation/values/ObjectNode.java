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
import me.topchetoeu.jscript.compilation.CompoundNode;
import me.topchetoeu.jscript.compilation.FunctionNode;
import me.topchetoeu.jscript.compilation.FunctionValueNode;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;


public class ObjectNode extends Node {
    public static class ObjProp {
        public final String name;
        public final String access;
        public final FunctionValueNode func;

        public ObjProp(String name, String access, FunctionValueNode func) {
            this.name = name;
            this.access = access;
            this.func = func;
        }
    }

    public final Map<String, Node> map;
    public final Map<String, FunctionNode> getters;
    public final Map<String, FunctionNode> setters;

    @Override public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.loadObj());

        for (var el : map.entrySet()) {
            target.add(Instruction.dup());
            var val = el.getValue();
            FunctionNode.compileWithName(val, target, true, el.getKey().toString());
            target.add(Instruction.storeMember(el.getKey()));
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

    public ObjectNode(Location loc, Map<String, Node> map, Map<String, FunctionNode> getters, Map<String, FunctionNode> setters) {
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
    private static ParseRes<ObjectNode.ObjProp> parseObjectProp(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var access = Parsing.parseIdentifier(src, i + n);
        if (!access.isSuccess()) return ParseRes.failed();
        if (!access.result.equals("get") && !access.result.equals("set")) return ParseRes.failed();
        n += access.n;

        var name = parsePropName(src, i + n);
        if (!name.isSuccess()) return name.chainError(src.loc(i + n), "Expected a property name after '" + access + "'");
        n += name.n;

        var params = JavaScript.parseParameters(src, i + n);
        if (!params.isSuccess()) return params.chainError(src.loc(i + n), "Expected an argument list");
        n += params.n;

        var body = CompoundNode.parse(src, i + n);
        if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected a compound statement for property accessor.");
        n += body.n;

        var end = src.loc(i + n - 1);

        return ParseRes.res(new ObjProp(
            name.result, access.result,
            new FunctionValueNode(loc, end, params.result, body.result, access + " " + name.result.toString())
        ), n);
    }

    public static ParseRes<ObjectNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "{")) return ParseRes.failed();
        n++;
        n += Parsing.skipEmpty(src, i + n);

        var values = new LinkedHashMap<String, Node>();
        var getters = new LinkedHashMap<String, FunctionNode>();
        var setters = new LinkedHashMap<String, FunctionNode>();

        if (src.is(i + n, "}")) {
            n++;
            return ParseRes.res(new ObjectNode(loc, values, getters, setters), n);
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
    
        return ParseRes.res(new ObjectNode(loc, values, getters, setters), n);
    }

}
