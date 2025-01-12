package me.topchetoeu.j2s.compilation.values.operations;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.JavaScript;
import me.topchetoeu.j2s.compilation.Node;
import me.topchetoeu.j2s.compilation.parsing.ParseRes;
import me.topchetoeu.j2s.compilation.parsing.Parsing;
import me.topchetoeu.j2s.compilation.parsing.Source;

public class LazyAndNode extends Node {
	public final Node first, second;

	@Override public void compileFunctions(CompileResult target) {
		first.compileFunctions(target);
		second.compileFunctions(target);
	}

	@Override public void compile(CompileResult target, boolean pollute) {
		first.compile(target, true);
		if (pollute) target.add(Instruction.dup());
		int start = target.temp();
		if (pollute) target.add(Instruction.discard());
		second.compile(target, pollute);
		target.set(start, Instruction.jmpIfNot(target.size() - start));
	}

	public LazyAndNode(Location loc, Node first, Node second) {
		super(loc);
		this.first = first;
		this.second = second;
	}


	public static ParseRes<LazyAndNode> parse(Source src, int i, Node prev, int precedence) {
		if (precedence < 4) return ParseRes.failed();
		var n = Parsing.skipEmpty(src, i);

		if (!src.is(i + n, "&&")) return ParseRes.failed();
		var loc = src.loc(i + n);
		n += 2;

		var res = JavaScript.parseExpression(src, i + n, 4);
		if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected a value after the '&&' operator.");
		n += res.n;

		return ParseRes.res(new LazyAndNode(loc, prev, res.result), n);
	}
}
