package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.values.constants.NumberStatement;

public class ChangeStatement extends Statement {
    public final AssignableStatement value;
    public final double addAmount;
    public final boolean postfix;

    @Override public void compile(CompileResult target, boolean pollute) {
        value.toAssign(new NumberStatement(loc(), -addAmount), Operation.SUBTRACT).compile(target, true);
        if (!pollute) target.add(Instruction.discard());
        else if (postfix) {
            target.add(Instruction.pushValue(addAmount));
            target.add(Instruction.operation(Operation.SUBTRACT));
        }
    }

    public ChangeStatement(Location loc, AssignableStatement value, double addAmount, boolean postfix) {
        super(loc);
        this.value = value;
        this.addAmount = addAmount;
        this.postfix = postfix;
    }

    public static ParseRes<ChangeStatement> parsePrefixIncrease(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "++")) return ParseRes.failed();
        n += 2;

        var res = JavaScript.parseExpression(src, i + n, 15);
        if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected assignable value after prefix operator.");
        else if (!(res.result instanceof AssignableStatement)) return ParseRes.error(src.loc(i + n), "Expected assignable value after prefix operator.");

        return ParseRes.res(new ChangeStatement(loc, (AssignableStatement)res.result, 1, false), n + res.n);
    }
    public static ParseRes<ChangeStatement> parsePrefixDecrease(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "--")) return ParseRes.failed();
        n += 2;

        var res = JavaScript.parseExpression(src, i + n, 15);
        if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected assignable value after prefix operator.");
        else if (!(res.result instanceof AssignableStatement)) return ParseRes.error(src.loc(i + n), "Expected assignable value after prefix operator.");

        return ParseRes.res(new ChangeStatement(loc, (AssignableStatement)res.result, -1, false), n + res.n);
    }

    public static ParseRes<ChangeStatement> parsePostfixIncrease(Source src, int i, Statement prev, int precedence) {
        if (precedence > 15) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "++")) return ParseRes.failed();
        if (!(prev instanceof AssignableStatement)) return ParseRes.error(src.loc(i + n), "Expected assignable value before suffix operator.");
        n += 2;

        return ParseRes.res(new ChangeStatement(loc, (AssignableStatement)prev, 1, true), n);
    }
    public static ParseRes<ChangeStatement> parsePostfixDecrease(Source src, int i, Statement prev, int precedence) {
        if (precedence > 15) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "--")) return ParseRes.failed();
        if (!(prev instanceof AssignableStatement)) return ParseRes.error(src.loc(i + n), "Expected assignable value before suffix operator.");
        n += 2;

        return ParseRes.res(new ChangeStatement(loc, (AssignableStatement)prev, -1, true), n);
    }
}
