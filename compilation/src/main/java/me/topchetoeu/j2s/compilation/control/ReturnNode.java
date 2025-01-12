package me.topchetoeu.j2s.compilation.control;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.JavaScript;
import me.topchetoeu.j2s.compilation.Node;
import me.topchetoeu.j2s.compilation.parsing.ParseRes;
import me.topchetoeu.j2s.compilation.parsing.Parsing;
import me.topchetoeu.j2s.compilation.parsing.Source;

public class ReturnNode extends Node {
	public final Node value;

	@Override public void compileFunctions(CompileResult target) {
		if (value != null) value.compileFunctions(target);
	}
	@Override public void compile(CompileResult target, boolean pollute) {
		if (value == null) target.add(Instruction.pushUndefined());
		else value.compile(target, true);
		target.add(Instruction.ret()).setLocation(loc());
	}

	public ReturnNode(Location loc, Node value) {
		super(loc);
		this.value = value;
	}

	public static ParseRes<ReturnNode> parse(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		if (!Parsing.isIdentifier(src, i + n, "return")) return ParseRes.failed();
		n += 6;

		var end = JavaScript.parseStatementEnd(src, i + n);
		if (end.isSuccess()) {
			n += end.n;
			return ParseRes.res(new ReturnNode(loc, null), n);
		}

		var val = JavaScript.parseExpression(src, i + n, 0);
		if (val.isError()) return val.chainError();
		n += val.n;

		end = JavaScript.parseStatementEnd(src, i + n);
		if (end.isSuccess()) {
			n += end.n;
			return ParseRes.res(new ReturnNode(loc, val.result), n);
		}
		else return end.chainError(src.loc(i + n), "Expected end of statement or a return value");
	}
}
