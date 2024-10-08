package me.topchetoeu.jscript.compilation.members;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.values.ObjectNode;
import me.topchetoeu.jscript.compilation.values.VariableNode;
import me.topchetoeu.jscript.compilation.values.constants.StringNode;

public class FieldMemberNode extends Node {
    public final Node key;
    public final Node value;

    @Override public void compile(CompileResult target, boolean pollute) {
        if (pollute) target.add(Instruction.dup());
        key.compile(target, true);

        if (value == null) target.add(Instruction.pushUndefined());
        else value.compile(target, true);

        target.add(Instruction.defField());
    }

    public FieldMemberNode(Location loc, Node key, Node value) {
        super(loc);
        this.key = key;
        this.value = value;
    }

    public static ParseRes<FieldMemberNode> parseObject(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var name = ObjectNode.parsePropName(src, i + n);
        if (!name.isSuccess()) return name.chainError();
        n += name.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ":")) return ParseRes.failed();
        n++;

        var value = JavaScript.parseExpression(src, i + n, 2);
        if (!value.isSuccess()) return value.chainError(src.loc(i + n), "Expected a value");
        n += value.n;

        return ParseRes.res(new FieldMemberNode(loc, name.result, value.result), n);
    }

    public static ParseRes<FieldMemberNode> parseShorthand(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var var = VariableNode.parse(src, i + n);
        if (!var.isSuccess()) return var.chainError();
        n += var.n;

        return ParseRes.res(new FieldMemberNode(loc, new StringNode(loc, var.result.name), var.result), n);
    }

    public static ParseRes<FieldMemberNode> parseClass(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var name = ObjectNode.parsePropName(src, i + n);
        if (!name.isSuccess()) return name.chainError();
        n += name.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "=")) {
            var end = JavaScript.parseStatement(src, i + n);
            if (!end.isSuccess()) return ParseRes.error(src.loc(i + n), "Expected an end of statement or a field initializer");
            n += end.n;

            return ParseRes.res(new FieldMemberNode(loc, name.result, null), n);
        }
        n++;

        var value = JavaScript.parseExpression(src, i + n, 2);
        if (!value.isSuccess()) return value.chainError(src.loc(i + n), "Expected a value");
        n += value.n;

        return ParseRes.res(new FieldMemberNode(loc, name.result, value.result), n);
    }
}