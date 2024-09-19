package me.topchetoeu.jscript.compilation.members;

import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.patterns.AssignTarget;
import me.topchetoeu.jscript.compilation.values.VariableNode;
import me.topchetoeu.jscript.compilation.values.constants.StringNode;
import me.topchetoeu.jscript.compilation.values.operations.AssignNode;

public class AssignShorthandNode extends Node {
    public final Node key;
    public final AssignTarget target;
    public final Node value;

    @Override public void compile(CompileResult target, boolean pollute) {
        throw new SyntaxException(loc(), "Unexpected assign shorthand in non-destructor context");
    }

    public AssignShorthandNode(Location loc, Node key, AssignTarget target, Node value) {
        super(loc);
        this.key = key;
        this.target = target;
        this.value = value;
    }

    public AssignTarget target() {
        return new AssignNode(loc(), target, value);
    }

    public static ParseRes<AssignShorthandNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var var = VariableNode.parse(src, i + n);
        if (!var.isSuccess()) return var.chainError();
        n += var.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "=")) return ParseRes.failed();
        n++;

        var value = JavaScript.parseExpression(src, i + n, 2);
        if (!value.isSuccess()) return value.chainError(src.loc(i + n), "Expected a shorthand initializer");
        n += value.n;

        return ParseRes.res(new AssignShorthandNode(loc, new StringNode(loc, var.result.name), var.result, value.result), n);
    }
}