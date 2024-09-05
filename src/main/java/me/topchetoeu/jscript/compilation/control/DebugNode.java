package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;

public class DebugNode extends Node {
    @Override public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.debug());
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public DebugNode(Location loc) {
        super(loc);
    }

    public static ParseRes<DebugNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "debugger")) return ParseRes.failed();
        n += 8;

        var end = JavaScript.parseStatementEnd(src, i + n);
        if (end.isSuccess()) {
            n += end.n;
            return ParseRes.res(new DebugNode(loc), n);
        }
        else return end.chainError(src.loc(i + n), "Expected end of statement");
    }

}
