package me.topchetoeu.j2s.compilation.control;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.parsing.Location;
import me.topchetoeu.j2s.common.parsing.ParseRes;
import me.topchetoeu.j2s.common.parsing.Parsing;
import me.topchetoeu.j2s.common.parsing.Source;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.JavaScript;
import me.topchetoeu.j2s.compilation.Node;

public class ThrowNode extends Node {
	public final Node value;

	@Override public void compileFunctions(CompileResult target) {
		value.compileFunctions(target);
	}
	@Override public void compile(CompileResult target, boolean pollute) {
		value.compile(target, true);
		target.add(Instruction.throwInstr()).setLocation(loc());
	}

	public ThrowNode(Location loc, Node value) {
		super(loc);
		this.value = value;
	}

	public static ParseRes<ThrowNode> parse(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		if (!Parsing.isIdentifier(src, i + n, "throw")) return ParseRes.failed();
		n += 5;

		var val = JavaScript.parseExpression(src, i + n, 0);
		if (val.isFailed()) return ParseRes.error(src.loc(i + n), "Expected a throw value");
		n += val.n;

		var end = JavaScript.parseStatementEnd(src, i + n);
		if (end.isSuccess()) {
			n += end.n;
			return ParseRes.res(new ThrowNode(loc, val.result), n);
		}
		else return end.chainError(src.loc(i + n), "Expected end of statement");
	}
}
