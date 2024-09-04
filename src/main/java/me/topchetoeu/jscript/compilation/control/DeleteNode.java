package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.values.VariableNode;
import me.topchetoeu.jscript.compilation.values.constants.BoolNode;
import me.topchetoeu.jscript.compilation.values.operations.IndexNode;

public class DeleteNode extends Node {
    public final Node key;
    public final Node value;

    @Override public void compile(CompileResult target, boolean pollute) {
        value.compile(target, true);
        key.compile(target, true);

        target.add(Instruction.delete());
        if (pollute) target.add(Instruction.pushValue(true));
    }

    public static ParseRes<? extends Node> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "delete")) return ParseRes.failed();
        n += 6;

        var valRes = JavaScript.parseExpression(src, i + n, 15);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'delete'");
        n += valRes.n;

        if (valRes.result instanceof IndexNode) {
            var index = (IndexNode)valRes.result;
            return ParseRes.res(new DeleteNode(loc, index.index, index.object), n);
        }
        else if (valRes.result instanceof VariableNode) {
            return ParseRes.error(src.loc(i + n), "A variable may not be deleted");
        }
        else return ParseRes.res(new BoolNode(loc, true), n);
    }

    public DeleteNode(Location loc, Node key, Node value) {
        super(loc);
        this.key = key;
        this.value = value;
    }
}
