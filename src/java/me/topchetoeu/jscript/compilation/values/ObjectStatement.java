package me.topchetoeu.jscript.compilation.values;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Parsing.ObjProp;
import me.topchetoeu.jscript.compilation.parsing.Token;

public class ObjectStatement extends Statement {
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

    private static ParseRes<String> parsePropName(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);

        if (Parsing.inBounds(tokens, i)) {
            var token = tokens.get(i);

            if (token.isNumber() || token.isIdentifier() || token.isString()) return ParseRes.res(token.rawValue, 1);
            else return ParseRes.error(loc, "Expected identifier, string or number literal.");
        }
        else return ParseRes.failed();
    }
    private static ParseRes<ObjProp> parseObjectProp(Filename filename, List<Token> tokens, int i) {
        var loc =Parsing. getLoc(filename, tokens, i);
        int n = 0;

        var accessRes = Parsing.parseIdentifier(tokens, i + n++);
        if (!accessRes.isSuccess()) return ParseRes.failed();
        var access = accessRes.result;
        if (!access.equals("get") && !access.equals("set")) return ParseRes.failed();

        var nameRes = parsePropName(filename, tokens, i + n);
        if (!nameRes.isSuccess()) return ParseRes.error(loc, "Expected a property name after '" + access + "'.");
        var name = nameRes.result;
        n += nameRes.n;

        var argsRes = Parsing.parseParamList(filename, tokens, i + n);
        if (!argsRes.isSuccess()) return ParseRes.error(loc, "Expected an argument list.", argsRes);
        n += argsRes.n;

        var res = CompoundStatement.parse(filename, tokens, i + n);
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected a compound statement for property accessor.", res);
        n += res.n;

        var end = Parsing.getLoc(filename, tokens, i + n - 1);

        return ParseRes.res(new ObjProp(
            name, access,
            new FunctionStatement(loc, end, access + " " + name.toString(), argsRes.result.toArray(String[]::new), false, res.result)
        ), n);
    }

    public static ParseRes<ObjectStatement> parse(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;
        if (!Parsing.isOperator(tokens, i + n++, Operator.BRACE_OPEN)) return ParseRes.failed();
    
        var values = new LinkedHashMap<String, Statement>();
        var getters = new LinkedHashMap<String, FunctionStatement>();
        var setters = new LinkedHashMap<String, FunctionStatement>();
    
        if (Parsing.isOperator(tokens, i + n, Operator.BRACE_CLOSE)) {
            n++;
            return ParseRes.res(new ObjectStatement(loc, values, getters, setters), n);
        }
    
        while (true) {
            var propRes = parseObjectProp(filename, tokens, i + n);
    
            if (propRes.isSuccess()) {
                n += propRes.n;
                if (propRes.result.access.equals("set")) {
                    setters.put(propRes.result.name, propRes.result.func);
                }
                else {
                    getters.put(propRes.result.name, propRes.result.func);
                }
            }
            else {
                var nameRes = parsePropName(filename, tokens, i + n);
                if (!nameRes.isSuccess()) return ParseRes.error(loc, "Expected a field name.", propRes);
                n += nameRes.n;
    
                if (!Parsing.isOperator(tokens, i + n++, Operator.COLON)) return ParseRes.error(loc, "Expected a colon.");
    
                var valRes = Parsing.parseValue(filename, tokens, i + n, 2);
                if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value in array list.", valRes);
                n += valRes.n;
    
                values.put(nameRes.result, valRes.result);
            }
    
            if (Parsing.isOperator(tokens, i + n, Operator.COMMA)) {
                n++;
                if (Parsing.isOperator(tokens, i + n, Operator.BRACE_CLOSE)) {
                    n++;
                    break;
                }
                continue;
            }
            else if (Parsing.isOperator(tokens, i + n, Operator.BRACE_CLOSE)) {
                n++;
                break;
            }
            else ParseRes.error(loc, "Expected a comma or a closing brace.");
        }
    
        return ParseRes.res(new ObjectStatement(loc, values, getters, setters), n);
    }

}
