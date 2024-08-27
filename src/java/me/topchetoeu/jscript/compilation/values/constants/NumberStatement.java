package me.topchetoeu.jscript.compilation.values.constants;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.ParseRes;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Source;

public class NumberStatement extends Statement {
    public final double value;

    @Override public boolean pure() { return true; }

    @Override public void compile(CompileResult target, boolean pollute) {
        if (pollute) target.add(Instruction.pushValue(value));
    }

    public NumberStatement(Location loc, double value) {
        super(loc);
        this.value = value;
    }

    public static double power(double a, long b) {
        if (b == 0) return 1;
        if (b == 1) return a;
        if (b < 0) return 1 / power(a, -b);

        if ((b & 1) == 0) return power(a * a, b / 2);
        else return a * power(a * a, b / 2);
    }

    public static ParseRes<NumberStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var res = Parsing.parseNumber(src, i + n);
        if (res.isSuccess()) return ParseRes.res(new NumberStatement(loc, res.result), n + res.n);
        else return res.chainError();
    }
}
