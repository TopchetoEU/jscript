package me.topchetoeu.j2s.compilation.values.constants;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.Node;
import me.topchetoeu.j2s.compilation.parsing.ParseRes;
import me.topchetoeu.j2s.compilation.parsing.Parsing;
import me.topchetoeu.j2s.compilation.parsing.Source;

public class NumberNode extends Node {
	public final double value;

	@Override public void compileFunctions(CompileResult target) {
	}

	@Override public void compile(CompileResult target, boolean pollute) {
		if (pollute) target.add(Instruction.pushValue(value));
	}

	public NumberNode(Location loc, double value) {
		super(loc);
		this.value = value;
	}

	public static ParseRes<NumberNode> parse(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		var res = Parsing.parseNumber(src, i + n, false);
		if (res.isSuccess()) return ParseRes.res(new NumberNode(loc, res.result), n + res.n);
		else return res.chainError();
	}
}
