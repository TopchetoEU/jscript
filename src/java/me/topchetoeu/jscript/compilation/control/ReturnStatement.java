package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.ParseRes;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Source;

public class ReturnStatement extends Statement {
    public final Statement value;

    @Override
    public void compile(CompileResult target, boolean pollute) {
        if (value == null) target.add(Instruction.pushUndefined());
        else value.compile(target, true);
        target.add(Instruction.ret()).setLocation(loc());
    }

    public ReturnStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }

    public static ParseRes<ReturnStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "return")) return ParseRes.failed();
        n += 6;

        var end = Parsing.parseStatementEnd(src, i + n);
        if (end.isSuccess()) {
            n += end.n;
            return ParseRes.res(new ReturnStatement(loc, null), n);
        }

        var val = Parsing.parseValue(src, i + n, 0);
        if (val.isFailed()) return ParseRes.error(src.loc(i + n), "Expected a value");
        n += val.n;

        end = Parsing.parseStatementEnd(src, i + n);
        if (end.isSuccess()) {
            n += end.n;
            return ParseRes.res(new ReturnStatement(loc, val.result), n);
        }
        else return end.chainError(src.loc(i + n), "Expected end of statement");
    }
}
