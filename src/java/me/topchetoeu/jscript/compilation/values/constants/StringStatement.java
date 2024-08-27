package me.topchetoeu.jscript.compilation.values.constants;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.ParseRes;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Source;

public class StringStatement extends Statement {
    public final String value;

    @Override public boolean pure() { return true; }

    @Override public void compile(CompileResult target, boolean pollute) {
        if (pollute) target.add(Instruction.pushValue(value));
    }

    public StringStatement(Location loc, String value) {
        super(loc);
        this.value = value;
    }

    public static ParseRes<StringStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var res = Parsing.parseString(src, i + n);
        if (res.isSuccess()) return ParseRes.res(new StringStatement(loc, res.result), n + res.n);
        else return res.chainError();
    }
}
