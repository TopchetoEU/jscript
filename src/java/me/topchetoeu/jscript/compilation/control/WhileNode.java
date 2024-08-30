package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.Instruction.Type;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;

public class WhileNode extends Node {
    public final Node condition, body;
    public final String label;

    @Override
    public void declare(CompileResult target) {
        body.declare(target);
    }
    @Override
    public void compile(CompileResult target, boolean pollute) {
        int start = target.size();
        condition.compile(target, true);
        int mid = target.temp();
        body.compile(target, false, BreakpointType.STEP_OVER);

        int end = target.size();

        replaceBreaks(target, label, mid + 1, end, start, end + 1);

        target.add(Instruction.jmp(start - end));
        target.set(mid, Instruction.jmpIfNot(end - mid + 1));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public static ParseRes<String> parseLabel(Source src, int i) {
        int n = Parsing.skipEmpty(src, i);

        var nameRes = Parsing.parseIdentifier(src, i + n);
        if (!nameRes.isSuccess()) return nameRes.chainError();
        n += nameRes.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ":")) return ParseRes.failed();
        n++;

        return ParseRes.res(nameRes.result, n);
    }

    public WhileNode(Location loc, String label, Node condition, Node body) {
        super(loc);
        this.label = label;
        this.condition = condition;
        this.body = body;
    }

    public static void replaceBreaks(CompileResult target, String label, int start, int end, int continuePoint, int breakPoint) {
        for (int i = start; i < end; i++) {
            var instr = target.get(i);
            if (instr.type == Type.NOP && instr.is(0, "cont") && (instr.get(1) == null || instr.is(1, label))) {
                target.set(i, Instruction.jmp(continuePoint - i));
            }
            if (instr.type == Type.NOP && instr.is(0, "break") && (instr.get(1) == null || instr.is(1, label))) {
                target.set(i, Instruction.jmp(breakPoint - i));
            }
        }
    }

    public static ParseRes<WhileNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var labelRes = WhileNode.parseLabel(src, i + n);
        n += labelRes.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!Parsing.isIdentifier(src, i + n, "while")) return ParseRes.failed();
        n += 5;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "(")) return ParseRes.error(src.loc(i + n), "Expected a open paren after 'while'.");
        n++;

        var condRes = JavaScript.parseExpression(src, i + n, 0);
        if (!condRes.isSuccess()) return condRes.chainError(src.loc(i + n), "Expected a while condition.");
        n += condRes.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a closing paren after while condition.");
        n++;

        var res = JavaScript.parseStatement(src, i + n);
        if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected a while body.");
        n += res.n;

        return ParseRes.res(new WhileNode(loc, labelRes.result, condRes.result, res.result), n);
    }
}
