package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.LabelContext;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;

public class ContinueNode extends Node {
    public final String label;

    @Override public void compile(CompileResult target, boolean pollute) {
        var res = LabelContext.getCont(target.env).getJump();
        if (res == null) {
            if (label != null) throw new SyntaxException(loc(), String.format("Undefined label '%s'", label));
            else throw new SyntaxException(loc(), "Illegal continue statement");
        }
        target.add(res);

        if (pollute) target.add(Instruction.pushUndefined());
    }

    public ContinueNode(Location loc, String label) {
        super(loc);
        this.label = label;
    }

    public static ParseRes<ContinueNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "continue")) return ParseRes.failed();
        n += 8;

        var end = JavaScript.parseStatementEnd(src, i + n);
        if (end.isSuccess()) {
            n += end.n;
            return ParseRes.res(new ContinueNode(loc, null), n);
        }

        var label = Parsing.parseIdentifier(src, i + n);
        if (label.isFailed()) return ParseRes.error(src.loc(i + n), "Expected a label name or an end of statement");
        n += label.n;

        end = JavaScript.parseStatementEnd(src, i + n);
        if (end.isSuccess()) {
            n += end.n;
            return ParseRes.res(new ContinueNode(loc, label.result), n);
        }
        else return end.chainError(src.loc(i + n), "Expected end of statement");
    }
}
