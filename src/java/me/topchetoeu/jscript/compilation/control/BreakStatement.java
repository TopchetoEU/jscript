package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.ES5;
import me.topchetoeu.jscript.compilation.Statement;

public class BreakStatement extends Statement {
    public final String label;

    @Override public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.nop("break", label));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public BreakStatement(Location loc, String label) {
        super(loc);
        this.label = label;
    }

    public static ParseRes<BreakStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "break")) return ParseRes.failed();
        n += 5;

        var end = ES5.parseStatementEnd(src, i + n);
        if (end.isSuccess()) {
            n += end.n;
            return ParseRes.res(new BreakStatement(loc, null), n);
        }

        var label = Parsing.parseIdentifier(src, i + n);
        if (label.isFailed()) return ParseRes.error(src.loc(i + n), "Expected a label name or an end of statement");
        n += label.n;

        end = ES5.parseStatementEnd(src, i + n);
        if (end.isSuccess()) {
            n += end.n;
            return ParseRes.res(new BreakStatement(loc, label.result), n);
        }
        else return end.chainError(src.loc(i + n), "Expected end of statement");
    }
}
