package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.ES5;
import me.topchetoeu.jscript.compilation.Statement;

public class LazyOrStatement extends Statement {
    public final Statement first, second;

    @Override public boolean pure() { return first.pure() && second.pure(); }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        first.compile(target, true);
        if (pollute) target.add(Instruction.dup());
        int start = target.temp();
        if (pollute) target.add(Instruction.discard());
        second.compile(target, pollute);
        target.set(start, Instruction.jmpIf(target.size() - start));
    }

    public LazyOrStatement(Location loc, Statement first, Statement second) {
        super(loc);
        this.first = first;
        this.second = second;
    }


    public static ParseRes<LazyOrStatement> parse(Source src, int i, Statement prev, int precedence) {
        if (precedence < 3) return ParseRes.failed();
        var n = Parsing.skipEmpty(src, i);

        if (!src.is(i + n, "||")) return ParseRes.failed();
        var loc = src.loc(i + n);
        n += 2;

        var res = ES5.parseExpression(src, i + n, 4);
        if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected a value after the '||' operator.");
        n += res.n;

        return ParseRes.res(new LazyOrStatement(loc, prev, res.result), n);
    }
}
