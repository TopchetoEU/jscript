package me.topchetoeu.jscript.compilation.values.operations;

import java.util.ArrayList;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;

public class CallNode extends Node {
	public final Node func;
	public final Node[] args;
	public final boolean isNew;

	@Override public void compileFunctions(CompileResult target) {
		func.compileFunctions(target);
		for (var arg : args) arg.compileFunctions(target);
	}

	@Override public void compile(CompileResult target, boolean pollute, BreakpointType type) {
		if (!isNew && func instanceof IndexNode indexNode) {
			var obj = indexNode.object;
			var index = indexNode.index;

			obj.compile(target, true);
			target.add(Instruction.dup());
			IndexNode.indexLoad(target, index, true);

			for (var arg : args) arg.compile(target, true);

			target.add(Instruction.call(args.length, true));

			target.setLocationAndDebug(loc(), type);
		}
		else {
			func.compile(target, true);
			for (var arg : args) arg.compile(target, true);

			if (isNew) target.add(Instruction.callNew(args.length)).setLocationAndDebug(loc(), type);
			else target.add(Instruction.call(args.length, false)).setLocationAndDebug(loc(), type);
		}
		if (!pollute) target.add(Instruction.discard());
	}
	@Override public void compile(CompileResult target, boolean pollute) {
		compile(target, pollute, BreakpointType.STEP_IN);
	}

	public CallNode(Location loc, boolean isNew, Node func, Node ...args) {
		super(loc);
		this.isNew = isNew;
		this.func = func;
		this.args = args;
	}

	public static ParseRes<CallNode> parseCall(Source src, int i, Node prev, int precedence) {
		if (precedence > 17) return ParseRes.failed();

		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		if (!src.is(i + n, "(")) return ParseRes.failed();
		n++;

		var args = new ArrayList<Node>();
		boolean prevArg = false;

		while (true) {
			var argRes = JavaScript.parseExpression(src, i + n, 2);
			n += argRes.n;
			n += Parsing.skipEmpty(src, i + n);

			if (argRes.isSuccess()) {
				args.add(argRes.result);
				prevArg = true;
			}
			else if (argRes.isError()) return argRes.chainError();

			if (prevArg && src.is(i + n, ",")) {
				prevArg = false;
				n++;
			}
			else if (src.is(i + n, ")")) {
				n++;
				break;
			}
			else if (prevArg) return ParseRes.error(src.loc(i + n), "Expected a comma or a closing paren");
			else return ParseRes.error(src.loc(i + n), "Expected an expression or a closing paren");
		}

		return ParseRes.res(new CallNode(loc, false, prev, args.toArray(new Node[0])), n);
	}
	public static ParseRes<CallNode> parseNew(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		if (!Parsing.isIdentifier(src, i + n, "new")) return ParseRes.failed();
		n += 3;

		var valRes = JavaScript.parseExpression(src, i + n, 18);
		if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'new' keyword.");
		n += valRes.n;

		var callRes = CallNode.parseCall(src, i + n, valRes.result, 0);
		if (callRes.isFailed()) return ParseRes.res(new CallNode(loc, true, valRes.result), n);
		if (callRes.isError()) return callRes.chainError();
		n += callRes.n;

		return ParseRes.res(new CallNode(loc, true, callRes.result.func, callRes.result.args), n);
	}
}
