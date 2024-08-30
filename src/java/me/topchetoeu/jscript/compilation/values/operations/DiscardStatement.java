package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Statement;

public class DiscardStatement extends Statement {
    public final Statement value;

    @Override public boolean pure() { return value.pure(); }

    @Override public void compile(CompileResult target, boolean pollute) {
        if (value != null) value.compile(target, false);
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public DiscardStatement(Location loc, Statement val) {
        super(loc);
        this.value = val;
    }

    public static ParseRes<DiscardStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "void")) return ParseRes.failed();
        n += 4;

        var valRes = JavaScript.parseExpression(src, i + n, 14);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'void' keyword.");
        n += valRes.n;

        return ParseRes.res(new DiscardStatement(loc, valRes.result), n);
    }
}
