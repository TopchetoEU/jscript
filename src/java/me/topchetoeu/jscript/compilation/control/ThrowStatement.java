package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Statement;

public class ThrowStatement extends Statement {
    public final Statement value;

    @Override
    public void compile(CompileResult target, boolean pollute) {
        value.compile(target, true);
        target.add(Instruction.throwInstr()).setLocation(loc());
    }

    public ThrowStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }

    public static ParseRes<ThrowStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "throw")) return ParseRes.failed();
        n += 5;

        var end = JavaScript.parseStatementEnd(src, i + n);
        if (end.isSuccess()) {
            n += end.n;
            return ParseRes.res(new ThrowStatement(loc, null), n);
        }

        var val = JavaScript.parseExpression(src, i + n, 0);
        if (val.isFailed()) return ParseRes.error(src.loc(i + n), "Expected a value");
        n += val.n;

        end = JavaScript.parseStatementEnd(src, i + n);
        if (end.isSuccess()) {
            n += end.n;
            return ParseRes.res(new ThrowStatement(loc, val.result), n);
        }
        else return end.chainError(src.loc(i + n), "Expected end of statement");
    }
}
