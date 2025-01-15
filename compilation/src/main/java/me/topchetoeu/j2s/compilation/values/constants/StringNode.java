package me.topchetoeu.j2s.compilation.values.constants;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.Node;
import me.topchetoeu.j2s.compilation.parsing.ParseRes;
import me.topchetoeu.j2s.compilation.parsing.Parsing;
import me.topchetoeu.j2s.compilation.parsing.Source;

public class StringNode extends Node {
	public final String value;

	@Override public void compileFunctions(CompileResult target) {
	}

	@Override public void compile(CompileResult target, boolean pollute) {
		if (pollute) target.add(Instruction.pushValue(value));
	}

	public StringNode(Location loc, String value) {
		super(loc);
		this.value = value;
	}

	public static ParseRes<StringNode> parse(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		var res = Parsing.parseString(src, i + n);
		if (res.isSuccess()) return ParseRes.res(new StringNode(loc, res.result), n + res.n);
		else return res.chainError();
	}
}
