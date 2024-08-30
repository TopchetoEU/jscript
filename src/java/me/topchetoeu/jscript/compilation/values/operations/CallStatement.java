package me.topchetoeu.jscript.compilation.values.operations;

import java.util.ArrayList;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.json.JSON;
import me.topchetoeu.jscript.common.json.JSONElement;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.values.ArrayStatement;
import me.topchetoeu.jscript.compilation.values.ObjectStatement;
import me.topchetoeu.jscript.compilation.values.VariableStatement;
import me.topchetoeu.jscript.compilation.values.constants.BoolStatement;
import me.topchetoeu.jscript.compilation.values.constants.NumberStatement;
import me.topchetoeu.jscript.compilation.values.constants.StringStatement;

public class CallStatement extends Statement {
    public static boolean ATTACH_NAME = true;

    public final Statement func;
    public final Statement[] args;
    public final boolean isNew;

    private String generateName(Statement func, Statement index) {
        String res = "(intermediate value)";
        boolean shouldParen = false;

        if (func instanceof ObjectStatement) {
            var obj = (ObjectStatement)func;

            shouldParen = true;

            if (obj.getters.size() > 0 || obj.setters.size() > 0 || obj.map.size() > 0) res = "{}";
            else res = "{(intermediate value)}";
        }
        else if (func instanceof StringStatement) {
            res = JSON.stringify(JSONElement.string(((StringStatement)func).value));
        }
        else if (func instanceof NumberStatement) {
            res = JSON.stringify(JSONElement.number(((NumberStatement)func).value));
        }
        else if (func instanceof BoolStatement) {
            res = ((BoolStatement)func).value ? "true" : "false";
        }
        else if (func instanceof VariableStatement) {
            res = ((VariableStatement)func).name;
        }
        else if (func instanceof VariableIndexStatement) {
            var i = ((VariableIndexStatement)func).index;

            if (i == 0) res = "this";
            else if (i == 1) res = "arguments";
        }
        else if (func instanceof ArrayStatement) {
            var els = new ArrayList<String>();

            for (var el : ((ArrayStatement)func).statements) {
                if (el != null) els.add(generateName(el, null));
                else els.add("(intermediate value)");
            }

            res = "[" + String.join(",", els) + "]";
        }

        if (index == null) return res;

        if (shouldParen) res = "(" + res + ")";

        if (index instanceof StringStatement) {
            var val = ((StringStatement)index).value;
            var bracket = JSON.stringify(JSONElement.string(val));

            if (!bracket.substring(1, bracket.length() - 1).equals(val)) return res + "[" + bracket + "]";
            if (Parsing.parseIdentifier(new Source(null, val), 0).n != val.length()) return res + "[" + bracket + "]";

            return res + "." + val;
        }

        return res + "[" + generateName(index, null) + "]";
    }

    @Override public void compile(CompileResult target, boolean pollute, BreakpointType type) {
        if (!isNew && func instanceof IndexStatement) {
            var obj = ((IndexStatement)func).object;
            var index = ((IndexStatement)func).index;
            String name = "";

            obj.compile(target, true);
            index.compile(target, true);
            for (var arg : args) arg.compile(target, true);

            if (ATTACH_NAME) name = generateName(obj, index);

            target.add(Instruction.callMember(args.length, name)).setLocationAndDebug(loc(), type);
        }
        else {
            String name = "";

            func.compile(target, true);
            for (var arg : args) arg.compile(target, true);

            if (ATTACH_NAME) name = generateName(func, null);

            if (isNew) target.add(Instruction.callNew(args.length, name)).setLocationAndDebug(loc(), type);
            else target.add(Instruction.call(args.length, name)).setLocationAndDebug(loc(), type);
        }
        if (!pollute) target.add(Instruction.discard());
    }
    @Override public void compile(CompileResult target, boolean pollute) {
        compile(target, pollute, BreakpointType.STEP_IN);
    }

    public CallStatement(Location loc, boolean isNew, Statement func, Statement ...args) {
        super(loc);
        this.isNew = isNew;
        this.func = func;
        this.args = args;
    }

    public static ParseRes<CallStatement> parseCall(Source src, int i, Statement prev, int precedence) {
        if (precedence > 17) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "(")) return ParseRes.failed();
        n++;

        var args = new ArrayList<Statement>();
        boolean prevArg = false;

        while (true) {
            var argRes = JavaScript.parseExpression(src, i + n, 2);
            n += argRes.n;
            n += Parsing.skipEmpty(src, i + n);

            if (argRes.isSuccess()) {
                args.add(argRes.result);
                prevArg = true;
            }
            else if (argRes.isError()) return argRes.chainError();
            else if (prevArg && src.is(i + n, ",")) {
                prevArg = false;
                n++;
            }
            else if (src.is(i + n, ")")) {
                n++;
                break;
            }
            else if (prevArg) return ParseRes.error(src.loc(i + n), "Expected a comma or a closing paren");
            else return ParseRes.error(src.loc(i + n), "Expected an expression or a closing paren");
        }

        return ParseRes.res(new CallStatement(loc, false, prev, args.toArray(Statement[]::new)), n);
    }
    public static ParseRes<CallStatement> parseNew(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "new")) return ParseRes.failed();
        n += 3;

        var valRes = JavaScript.parseExpression(src, i + n, 18);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'new' keyword.");
        n += valRes.n;

        var callRes = CallStatement.parseCall(src, i + n, valRes.result, 0);
        if (callRes.isFailed()) return ParseRes.res(new CallStatement(loc, true, valRes.result), n);
        if (callRes.isError()) return callRes.chainError();
        n += callRes.n;

        return ParseRes.res(new CallStatement(loc, true, callRes.result.func, callRes.result.args), n);
    }
}
