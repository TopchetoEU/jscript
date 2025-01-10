package me.topchetoeu.j2s.compilation.values.operations;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.Operation;
import me.topchetoeu.j2s.common.parsing.Location;
import me.topchetoeu.j2s.common.parsing.ParseRes;
import me.topchetoeu.j2s.common.parsing.Parsing;
import me.topchetoeu.j2s.common.parsing.Source;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.Node;
import me.topchetoeu.j2s.compilation.patterns.ChangeTarget;
import me.topchetoeu.j2s.compilation.values.constants.NumberNode;

public class PostfixNode extends ChangeNode {
	@Override public void compileFunctions(CompileResult target) {
		((Node)changable).compileFunctions(target);
	}

	@Override public void compile(CompileResult target, boolean pollute) {
		super.compile(target, pollute);

		if (pollute) {
			value.compile(target, true);
			target.add(Instruction.operation(Operation.ADD));
		}
	}

	public PostfixNode(Location loc, ChangeTarget value, double addAmount) {
		super(loc, value, new NumberNode(loc, -addAmount), Operation.SUBTRACT);
	}

	public static ParseRes<ChangeNode> parsePostfixIncrease(Source src, int i, Node prev, int precedence) {
		if (precedence > 15) return ParseRes.failed();

		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		if (!src.is(i + n, "++")) return ParseRes.failed();
		if (!(prev instanceof ChangeTarget)) return ParseRes.error(src.loc(i + n), "Expected assignable value before suffix operator.");
		n += 2;

		return ParseRes.res(new PostfixNode(loc, (ChangeTarget)prev, 1), n);
	}
	public static ParseRes<ChangeNode> parsePostfixDecrease(Source src, int i, Node prev, int precedence) {
		if (precedence > 15) return ParseRes.failed();

		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		if (!src.is(i + n, "--")) return ParseRes.failed();
		if (!(prev instanceof ChangeTarget)) return ParseRes.error(src.loc(i + n), "Expected assignable value before suffix operator.");
		n += 2;

		return ParseRes.res(new PostfixNode(loc, (ChangeTarget)prev, -1), n);
	}
}
