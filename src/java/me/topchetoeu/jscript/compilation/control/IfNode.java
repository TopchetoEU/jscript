package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;

public class IfNode extends Node {
    public final Node condition, body, elseBody;

    @Override
    public void declare(CompileResult target) {
        body.declare(target);
        if (elseBody != null) elseBody.declare(target);
    }

    @Override public void compile(CompileResult target, boolean pollute, BreakpointType breakpoint) {
        condition.compile(target, true, breakpoint);

        if (elseBody == null) {
            int i = target.temp();
            body.compile(target, pollute, breakpoint);
            int endI = target.size();
            target.set(i, Instruction.jmpIfNot(endI - i));
        }
        else {
            int start = target.temp();
            body.compile(target, pollute, breakpoint);
            int mid = target.temp();
            elseBody.compile(target, pollute, breakpoint);
            int end = target.size();

            target.set(start, Instruction.jmpIfNot(mid - start + 1));
            target.set(mid, Instruction.jmp(end - mid));
        }
    }
    @Override public void compile(CompileResult target, boolean pollute) {
        compile(target, pollute, BreakpointType.STEP_IN);
    }

    public IfNode(Location loc, Node condition, Node body, Node elseBody) {
        super(loc);
        this.condition = condition;
        this.body = body;
        this.elseBody = elseBody;
    }

    public static ParseRes<IfNode> parseTernary(Source src, int i, Node prev, int precedence) {
        if (precedence > 2) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);

        if (!src.is(i + n, "?")) return ParseRes.failed();
        var loc = src.loc(i + n);
        n++;

        var a = JavaScript.parseExpression(src, i + n, 2);
        if (!a.isSuccess()) return a.chainError(src.loc(i + n), "Expected a value after the ternary operator.");
        n += a.n;
        n += Parsing.skipEmpty(src, i);

        if (!src.is(i + n, ":")) return ParseRes.failed();
        n++;

        var b = JavaScript.parseExpression(src, i + n, 2);
        if (!b.isSuccess()) return b.chainError(src.loc(i + n), "Expected a second value after the ternary operator.");
        n += b.n;

        return ParseRes.res(new IfNode(loc, prev, a.result, b.result), n);
    }
    public static ParseRes<IfNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "if")) return ParseRes.failed();
        n += 2;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "(")) return ParseRes.error(src.loc(i + n), "Expected a open paren after 'if'.");
        n++;

        var condRes = JavaScript.parseExpression(src, i + n, 0);
        if (!condRes.isSuccess()) return condRes.chainError(src.loc(i + n), "Expected an if condition.");
        n += condRes.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a closing paren after if condition.");
        n++;

        var res = JavaScript.parseStatement(src, i + n);
        if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected an if body.");
        n += res.n;

        var elseKw = Parsing.parseIdentifier(src, i + n, "else");
        if (!elseKw.isSuccess()) return ParseRes.res(new IfNode(loc, condRes.result, res.result, null), n);
        n += elseKw.n;

        var elseRes = JavaScript.parseStatement(src, i + n);
        if (!elseRes.isSuccess()) return elseRes.chainError(src.loc(i + n), "Expected an else body.");
        n += elseRes.n;

        return ParseRes.res(new IfNode(loc, condRes.result, res.result, elseRes.result), n);
    }

}
