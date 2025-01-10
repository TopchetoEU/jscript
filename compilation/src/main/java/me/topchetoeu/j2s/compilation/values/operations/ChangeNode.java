package me.topchetoeu.j2s.compilation.values.operations;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.Operation;
import me.topchetoeu.j2s.common.parsing.Location;
import me.topchetoeu.j2s.common.parsing.ParseRes;
import me.topchetoeu.j2s.common.parsing.Parsing;
import me.topchetoeu.j2s.common.parsing.Source;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.JavaScript;
import me.topchetoeu.j2s.compilation.Node;
import me.topchetoeu.j2s.compilation.patterns.ChangeTarget;
import me.topchetoeu.j2s.compilation.values.constants.NumberNode;

public class ChangeNode extends Node {
	public final ChangeTarget changable;
	public final Node value;
	public final Operation op;

	@Override public void compileFunctions(CompileResult target) {
		((Node)changable).compileFunctions(target);
		value.compileFunctions(target);
	}

	@Override public void compile(CompileResult target, boolean pollute) {
		changable.beforeChange(target);
		value.compile(target, true);
		target.add(Instruction.operation(op));
		changable.afterAssign(target, pollute);
	}

	public ChangeNode(Location loc, ChangeTarget changable, Node value, Operation op) {
		super(loc);
		this.changable = changable;
		this.value = value;
		this.op = op;
	}

	public static ParseRes<ChangeNode> parsePrefixIncrease(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		if (!src.is(i + n, "++")) return ParseRes.failed();
		n += 2;

		var res = JavaScript.parseExpression(src, i + n, 15);
		if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected assignable value after prefix operator.");
		else if (!(res.result instanceof ChangeTarget)) return ParseRes.error(src.loc(i + n), "Expected assignable value after prefix operator.");

		return ParseRes.res(new ChangeNode(loc, (ChangeTarget)res.result, new NumberNode(loc, -1), Operation.SUBTRACT), n + res.n);
	}
	public static ParseRes<ChangeNode> parsePrefixDecrease(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		if (!src.is(i + n, "--")) return ParseRes.failed();
		n += 2;

		var res = JavaScript.parseExpression(src, i + n, 15);
		if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected assignable value after prefix operator.");
		else if (!(res.result instanceof ChangeTarget)) return ParseRes.error(src.loc(i + n), "Expected assignable value after prefix operator.");

		return ParseRes.res(new ChangeNode(loc, (ChangeTarget)res.result, new NumberNode(loc, 1), Operation.SUBTRACT), n + res.n);
	}
}
