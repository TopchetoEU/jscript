package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.ES5;
import me.topchetoeu.jscript.compilation.Statement;

public class DoWhileStatement extends Statement {
    public final Statement condition, body;
    public final String label;

    @Override
    public void declare(CompileResult target) {
        body.declare(target);
    }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        int start = target.size();
        body.compile(target, false, BreakpointType.STEP_OVER);
        int mid = target.size();
        condition.compile(target, true, BreakpointType.STEP_OVER);
        int end = target.size();

        WhileStatement.replaceBreaks(target, label, start, mid - 1, mid, end + 1);
        target.add(Instruction.jmpIf(start - end));
    }

    public DoWhileStatement(Location loc, String label, Statement condition, Statement body) {
        super(loc);
        this.label = label;
        this.condition = condition;
        this.body = body;
    }

    public static ParseRes<DoWhileStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var labelRes = WhileStatement.parseLabel(src, i + n);
        n += labelRes.n;

        if (!Parsing.isIdentifier(src, i + n, "do")) return ParseRes.failed();
        n += 2;

        var bodyRes = ES5.parseStatement(src, i + n);
        if (!bodyRes.isSuccess()) return bodyRes.chainError(src.loc(i + n), "Expected a do-while body.");
        n += bodyRes.n;

        if (!Parsing.isIdentifier(src, i + n, "while")) return ParseRes.failed();
        n += 5;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "(")) return ParseRes.error(src.loc(i + n), "Expected a open paren after 'while'.");
        n++;

        var condRes = ES5.parseExpression(src, i + n, 0);
        if (!condRes.isSuccess()) return condRes.chainError(src.loc(i + n), "Expected a do-while condition.");
        n += condRes.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a closing paren after do-while condition.");
        n++;

        var end = ES5.parseStatementEnd(src, i + n);
        if (end.isSuccess()) {
            n += end.n;
            return ParseRes.res(new DoWhileStatement(loc, labelRes.result, condRes.result, bodyRes.result), n);
        }
        else return end.chainError(src.loc(i + n), "Expected end of statement");
    }

}
