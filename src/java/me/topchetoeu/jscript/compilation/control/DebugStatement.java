package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.ParseRes;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Source;

public class DebugStatement extends Statement {
    @Override public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.debug());
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public DebugStatement(Location loc) {
        super(loc);
    }

    public static ParseRes<DebugStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "debugger")) return ParseRes.failed();
        n += 8;

        var end = Parsing.parseStatementEnd(src, i + n);
        if (end.isSuccess()) {
            n += end.n;
            return ParseRes.res(new DebugStatement(loc), n);
        }
        else return end.chainError(src.loc(i + n), "Expected end of statement");
    }

}
