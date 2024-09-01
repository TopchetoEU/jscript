package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.DeferredIntSupplier;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.LabelContext;
import me.topchetoeu.jscript.compilation.Node;

public class IfNode extends Node {
    public final Node condition, body, elseBody;
    public final String label;

    @Override public void resolve(CompileResult target) {
        body.resolve(target);
        if (elseBody != null) elseBody.resolve(target);
    }

    @Override public void compile(CompileResult target, boolean pollute, BreakpointType breakpoint) {
        condition.compile(target, true, breakpoint);

        if (elseBody == null) {
            int start = target.temp();
            var end = new DeferredIntSupplier();

            LabelContext.getBreak(target.env).push(loc(), label, end);

            var subtarget = target.subtarget();
            subtarget.add(() -> Instruction.stackAlloc(subtarget.scope.allocCount()));

            body.compile(subtarget, false, BreakpointType.STEP_OVER);

            subtarget.scope.end();
            subtarget.add(Instruction.stackFree(subtarget.scope.allocCount()));

            LabelContext.getBreak(target.env).pop(label);

            int endI = target.size();
            end.set(endI);

            target.set(start, Instruction.jmpIfNot(endI - start));
        }
        else {
            int start = target.temp();
            var end = new DeferredIntSupplier();

            LabelContext.getBreak(target.env).push(loc(), label, end);

            var bodyTarget = target.subtarget();
            bodyTarget.add(() -> Instruction.stackAlloc(bodyTarget.scope.allocCount()));

            body.compile(bodyTarget, false, BreakpointType.STEP_OVER);

            bodyTarget.scope.end();
            bodyTarget.add(Instruction.stackFree(bodyTarget.scope.allocCount()));

            int mid = target.temp();

            var elseTarget = target.subtarget();
            elseTarget.add(() -> Instruction.stackAlloc(elseTarget.scope.allocCount()));

            body.compile(elseTarget, false, BreakpointType.STEP_OVER);

            elseTarget.scope.end();
            elseTarget.add(Instruction.stackFree(elseTarget.scope.allocCount()));

            LabelContext.getBreak(target.env).pop(label);

            int endI = target.size();
            end.set(endI);

            target.set(start, Instruction.jmpIfNot(mid - start + 1));
            target.set(mid, Instruction.jmp(endI - mid));
        }
    }
    @Override public void compile(CompileResult target, boolean pollute) {
        compile(target, pollute, BreakpointType.STEP_IN);
    }

    public IfNode(Location loc, Node condition, Node body, Node elseBody, String label) {
        super(loc);
        this.condition = condition;
        this.body = body;
        this.elseBody = elseBody;
        this.label = label;
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

        return ParseRes.res(new IfNode(loc, prev, a.result, b.result, null), n);
    }
    public static ParseRes<IfNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var label = JavaScript.parseLabel(src, i + n);
        n += label.n;
        n += Parsing.skipEmpty(src, i + n);

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
        if (!elseKw.isSuccess()) return ParseRes.res(new IfNode(loc, condRes.result, res.result, null, label.result), n);
        n += elseKw.n;

        var elseRes = JavaScript.parseStatement(src, i + n);
        if (!elseRes.isSuccess()) return elseRes.chainError(src.loc(i + n), "Expected an else body.");
        n += elseRes.n;

        return ParseRes.res(new IfNode(loc, condRes.result, res.result, elseRes.result, label.result), n);
    }
}
