package me.topchetoeu.j2s.compilation.values.constants;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.parsing.Location;
import me.topchetoeu.j2s.common.parsing.ParseRes;
import me.topchetoeu.j2s.common.parsing.Parsing;
import me.topchetoeu.j2s.common.parsing.Source;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.Node;

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

	public static double power(double a, long b) {
		if (b == 0) return 1;
		if (b == 1) return a;
		if (b < 0) return 1 / power(a, -b);

		if ((b & 1) == 0) return power(a * a, b / 2);
		else return a * power(a * a, b / 2);
	}

	public static ParseRes<NumberNode> parse(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		var res = Parsing.parseNumber(src, i + n, false);
		if (res.isSuccess()) return ParseRes.res(new NumberNode(loc, res.result), n + res.n);
		else return res.chainError();
	}
}
