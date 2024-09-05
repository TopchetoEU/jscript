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
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.values.ArgumentsNode;
import me.topchetoeu.jscript.compilation.values.ArrayNode;
import me.topchetoeu.jscript.compilation.values.ObjectNode;
import me.topchetoeu.jscript.compilation.values.ThisNode;
import me.topchetoeu.jscript.compilation.values.VariableNode;
import me.topchetoeu.jscript.compilation.values.constants.BoolNode;
import me.topchetoeu.jscript.compilation.values.constants.NumberNode;
import me.topchetoeu.jscript.compilation.values.constants.StringNode;

public class CallNode extends Node {
    public static boolean ATTACH_NAME = true;

    public final Node func;
    public final Node[] args;
    public final boolean isNew;

    private String generateName(Node func, Node index) {
        String res = "(intermediate value)";
        boolean shouldParen = false;

        if (func instanceof ObjectNode) {
            var obj = (ObjectNode)func;

            shouldParen = true;

            if (obj.getters.size() > 0 || obj.setters.size() > 0 || obj.map.size() > 0) res = "{}";
            else res = "{(intermediate value)}";
        }
        else if (func instanceof StringNode) {
            res = JSON.stringify(JSONElement.string(((StringNode)func).value));
        }
        else if (func instanceof NumberNode) {
            res = JSON.stringify(JSONElement.number(((NumberNode)func).value));
        }
        else if (func instanceof BoolNode) {
            res = ((BoolNode)func).value ? "true" : "false";
        }
        else if (func instanceof VariableNode) {
            res = ((VariableNode)func).name;
        }
        else if (func instanceof ThisNode) {
            res = "this";
        }
        else if (func instanceof ArgumentsNode) {
            res = "arguments";
        }
        else if (func instanceof ArrayNode) {
            var els = new ArrayList<String>();

            for (var el : ((ArrayNode)func).statements) {
                if (el != null) els.add(generateName(el, null));
                else els.add("(intermediate value)");
            }

            res = "[" + String.join(",", els) + "]";
        }

        if (index == null) return res;

        if (shouldParen) res = "(" + res + ")";

        if (index instanceof StringNode) {
            var val = ((StringNode)index).value;
            var bracket = JSON.stringify(JSONElement.string(val));

            if (!bracket.substring(1, bracket.length() - 1).equals(val)) return res + "[" + bracket + "]";
            if (Parsing.parseIdentifier(new Source(val), 0).n != val.length()) return res + "[" + bracket + "]";

            return res + "." + val;
        }

        return res + "[" + generateName(index, null) + "]";
    }

    @Override public void compile(CompileResult target, boolean pollute, BreakpointType type) {
        if (!isNew && func instanceof IndexNode) {
            var obj = ((IndexNode)func).object;
            var index = ((IndexNode)func).index;
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

    public CallNode(Location loc, boolean isNew, Node func, Node ...args) {
        super(loc);
        this.isNew = isNew;
        this.func = func;
        this.args = args;
    }

    public static ParseRes<CallNode> parseCall(Source src, int i, Node prev, int precedence) {
        if (precedence > 17) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "(")) return ParseRes.failed();
        n++;

        var args = new ArrayList<Node>();
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

        return ParseRes.res(new CallNode(loc, false, prev, args.toArray(Node[]::new)), n);
    }
    public static ParseRes<CallNode> parseNew(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "new")) return ParseRes.failed();
        n += 3;

        var valRes = JavaScript.parseExpression(src, i + n, 18);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'new' keyword.");
        n += valRes.n;

        var callRes = CallNode.parseCall(src, i + n, valRes.result, 0);
        if (callRes.isFailed()) return ParseRes.res(new CallNode(loc, true, valRes.result), n);
        if (callRes.isError()) return callRes.chainError();
        n += callRes.n;

        return ParseRes.res(new CallNode(loc, true, callRes.result.func, callRes.result.args), n);
    }
}
