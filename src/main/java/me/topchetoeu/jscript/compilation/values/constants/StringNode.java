package me.topchetoeu.jscript.compilation.values.constants;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;

public class StringNode extends Node {
    public final String value;

    @Override public void compile(CompileResult target, boolean pollute) {
        if (pollute) target.add(Instruction.pushValue(value));
    }

    public StringNode(Location loc, String value) {
        super(loc);
        this.value = value;
    }

    public static ParseRes<StringNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var res = Parsing.parseString(src, i + n);
        if (res.isSuccess()) return ParseRes.res(new StringNode(loc, res.result), n + res.n);
        else return res.chainError();
    }
}
