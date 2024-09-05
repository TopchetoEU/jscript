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

public class WhileNode extends Node {
    public final Node condition, body;
    public final String label;

    @Override public void resolve(CompileResult target) {
        body.resolve(target);
    }
    @Override public void compile(CompileResult target, boolean pollute) {
        int start = target.size();
        condition.compile(target, true);
        int mid = target.temp();

        var end = new DeferredIntSupplier();


        LabelContext.pushLoop(target.env, loc(), label, end, start);
        body.compile(target, false, BreakpointType.STEP_OVER);
        LabelContext.popLoop(target.env, label);

        var endI = target.size();
        end.set(endI + 1);

        target.add(Instruction.jmp(start - end.getAsInt()));
        target.set(mid, Instruction.jmpIfNot(end.getAsInt() - mid + 1));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public WhileNode(Location loc, String label, Node condition, Node body) {
        super(loc);
        this.label = label;
        this.condition = condition;
        this.body = body;
    }

    public static ParseRes<WhileNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var label = JavaScript.parseLabel(src, i + n);
        n += label.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!Parsing.isIdentifier(src, i + n, "while")) return ParseRes.failed();
        n += 5;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "(")) return ParseRes.error(src.loc(i + n), "Expected a open paren after 'while'.");
        n++;

        var cond = JavaScript.parseExpression(src, i + n, 0);
        if (!cond.isSuccess()) return cond.chainError(src.loc(i + n), "Expected a while condition.");
        n += cond.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a closing paren after while condition.");
        n++;

        var body = JavaScript.parseStatement(src, i + n);
        if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected a while body.");
        n += body.n;

        return ParseRes.res(new WhileNode(loc, label.result, cond.result, body.result), n);
    }
}
