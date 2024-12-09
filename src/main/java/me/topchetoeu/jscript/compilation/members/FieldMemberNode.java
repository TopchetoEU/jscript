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

public class FieldMemberNode implements Member {
    public final Location loc;
    public final Node key;
    public final Node value;

    @Override public Location loc() { return loc; }

	@Override public void compileFunctions(CompileResult target) {
		key.compileFunctions(target);
		value.compileFunctions(target);
	}

    @Override public void compile(CompileResult target, boolean pollute) {
        if (pollute) target.add(Instruction.dup());
        key.compile(target, true);

        if (value == null) target.add(Instruction.pushUndefined());
        else value.compile(target, true);

        target.add(Instruction.defField());
    }

    public FieldMemberNode(Location loc, Node key, Node value) {
        this.loc = loc;
        this.key = key;
        this.value = value;
    }

    public static ParseRes<FieldMemberNode> parse(Source src, int i) {
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
}