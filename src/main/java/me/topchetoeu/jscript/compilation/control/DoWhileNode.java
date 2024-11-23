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

public class DoWhileNode extends Node {
    public final Node condition, body;
    public final String label;

    @Override public void resolve(CompileResult target) {
        body.resolve(target);
    }

    @Override public void compile(CompileResult target, boolean pollute) {
        int start = target.size();
        var end = new DeferredIntSupplier();
        var mid = new DeferredIntSupplier();

        LabelContext.pushLoop(target.env, loc(), label, end, start);
        body.compile(target, false, BreakpointType.STEP_OVER);

        mid.set(target.size());
        condition.compile(target, true, BreakpointType.STEP_OVER);
        int endI = target.size();
        end.set(endI + 1);

        LabelContext.popLoop(target.env, label);

        target.add(Instruction.jmpIf(start - endI));
    }

    public DoWhileNode(Location loc, String label, Node condition, Node body) {
        super(loc);
        this.label = label;
        this.condition = condition;
        this.body = body;
    }

    public static ParseRes<DoWhileNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var labelRes = JavaScript.parseLabel(src, i + n);
        n += labelRes.n;

        if (!Parsing.isIdentifier(src, i + n, "do")) return ParseRes.failed();
        n += 2;

        var bodyRes = JavaScript.parseStatement(src, i + n);
        if (!bodyRes.isSuccess()) return bodyRes.chainError(src.loc(i + n), "Expected a do-while body.");
        n += bodyRes.n;

        if (!Parsing.isIdentifier(src, i + n, "while")) return ParseRes.failed();
        n += 5;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "(")) return ParseRes.error(src.loc(i + n), "Expected a open paren after 'while'.");
        n++;

        var condRes = JavaScript.parseExpression(src, i + n, 0);
        if (!condRes.isSuccess()) return condRes.chainError(src.loc(i + n), "Expected a do-while condition.");
        n += condRes.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a closing paren after do-while condition.");
        n++;

        var end = JavaScript.parseStatementEnd(src, i + n);
        if (end.isSuccess()) {
            n += end.n;
            return ParseRes.res(new DoWhileNode(loc, labelRes.result, condRes.result, bodyRes.result), n);
        }
        else return end.chainError(src.loc(i + n), "Expected end of statement");
    }

}
