package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;


public class LazyOrNode extends Node {
    public final Node first, second;

    @Override public void compile(CompileResult target, boolean pollute) {
        first.compile(target, true);
        if (pollute) target.add(Instruction.dup());
        int start = target.temp();
        if (pollute) target.add(Instruction.discard());
        second.compile(target, pollute);
        target.set(start, Instruction.jmpIf(target.size() - start));
    }

    public LazyOrNode(Location loc, Node first, Node second) {
        super(loc);
        this.first = first;
        this.second = second;
    }


    public static ParseRes<LazyOrNode> parse(Source src, int i, Node prev, int precedence) {
        if (precedence < 3) return ParseRes.failed();
        var n = Parsing.skipEmpty(src, i);

        if (!src.is(i + n, "||")) return ParseRes.failed();
        var loc = src.loc(i + n);
        n += 2;

        var res = JavaScript.parseExpression(src, i + n, 4);
        if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected a value after the '||' operator.");
        n += res.n;

        return ParseRes.res(new LazyOrNode(loc, prev, res.result), n);
    }
}
