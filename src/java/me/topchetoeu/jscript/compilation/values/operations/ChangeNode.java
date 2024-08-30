package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.AssignableNode;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.values.constants.NumberNode;

public class ChangeNode extends Node {
    public final AssignableNode value;
    public final double addAmount;
    public final boolean postfix;

    @Override public void compile(CompileResult target, boolean pollute) {
        value.toAssign(new NumberNode(loc(), -addAmount), Operation.SUBTRACT).compile(target, true);
        if (!pollute) target.add(Instruction.discard());
        else if (postfix) {
            target.add(Instruction.pushValue(addAmount));
            target.add(Instruction.operation(Operation.SUBTRACT));
        }
    }

    public ChangeNode(Location loc, AssignableNode value, double addAmount, boolean postfix) {
        super(loc);
        this.value = value;
        this.addAmount = addAmount;
        this.postfix = postfix;
    }

    public static ParseRes<ChangeNode> parsePrefixIncrease(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "++")) return ParseRes.failed();
        n += 2;

        var res = JavaScript.parseExpression(src, i + n, 15);
        if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected assignable value after prefix operator.");
        else if (!(res.result instanceof AssignableNode)) return ParseRes.error(src.loc(i + n), "Expected assignable value after prefix operator.");

        return ParseRes.res(new ChangeNode(loc, (AssignableNode)res.result, 1, false), n + res.n);
    }
    public static ParseRes<ChangeNode> parsePrefixDecrease(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "--")) return ParseRes.failed();
        n += 2;

        var res = JavaScript.parseExpression(src, i + n, 15);
        if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected assignable value after prefix operator.");
        else if (!(res.result instanceof AssignableNode)) return ParseRes.error(src.loc(i + n), "Expected assignable value after prefix operator.");

        return ParseRes.res(new ChangeNode(loc, (AssignableNode)res.result, -1, false), n + res.n);
    }

    public static ParseRes<ChangeNode> parsePostfixIncrease(Source src, int i, Node prev, int precedence) {
        if (precedence > 15) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "++")) return ParseRes.failed();
        if (!(prev instanceof AssignableNode)) return ParseRes.error(src.loc(i + n), "Expected assignable value before suffix operator.");
        n += 2;

        return ParseRes.res(new ChangeNode(loc, (AssignableNode)prev, 1, true), n);
    }
    public static ParseRes<ChangeNode> parsePostfixDecrease(Source src, int i, Node prev, int precedence) {
        if (precedence > 15) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "--")) return ParseRes.failed();
        if (!(prev instanceof AssignableNode)) return ParseRes.error(src.loc(i + n), "Expected assignable value before suffix operator.");
        n += 2;

        return ParseRes.res(new ChangeNode(loc, (AssignableNode)prev, -1, true), n);
    }
}
