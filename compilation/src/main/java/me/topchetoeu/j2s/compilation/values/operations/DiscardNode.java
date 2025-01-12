package me.topchetoeu.j2s.compilation.values.operations;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.JavaScript;
import me.topchetoeu.j2s.compilation.Node;
import me.topchetoeu.j2s.compilation.parsing.ParseRes;
import me.topchetoeu.j2s.compilation.parsing.Parsing;
import me.topchetoeu.j2s.compilation.parsing.Source;


public class DiscardNode extends Node {
	public final Node value;

	@Override public void compileFunctions(CompileResult target) {
		if (value != null) value.compileFunctions(target);
	}

	@Override public void compile(CompileResult target, boolean pollute) {
		if (value != null) value.compile(target, false);
		if (pollute) target.add(Instruction.pushUndefined());
	}

	public DiscardNode(Location loc, Node val) {
		super(loc);
		this.value = val;
	}

	public static ParseRes<DiscardNode> parse(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		if (!Parsing.isIdentifier(src, i + n, "void")) return ParseRes.failed();
		n += 4;

		var valRes = JavaScript.parseExpression(src, i + n, 14);
		if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'void' keyword.");
		n += valRes.n;

		return ParseRes.res(new DiscardNode(loc, valRes.result), n);
	}
}
