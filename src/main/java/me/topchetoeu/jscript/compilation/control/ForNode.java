package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.DeferredIntSupplier;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.LabelContext;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.VariableDeclareNode;

public class ForNode extends Node {
	public final Node declaration, assignment, condition, body;
	public final String label;

	@Override public void resolve(CompileResult target) {
		if (declaration != null) declaration.resolve(target);
		body.resolve(target);
	}
	@Override public void compileFunctions(CompileResult target) {
		if (declaration != null) declaration.compileFunctions(target);
		if (assignment != null) assignment.compileFunctions(target);
		if (condition != null) condition.compileFunctions(target);
		body.compileFunctions(target);
	}
	@Override public void compile(CompileResult target, boolean pollute) {
		if (declaration != null) declaration.compile(target, false, BreakpointType.STEP_OVER);

		var continueTarget = new DeferredIntSupplier();
		int start = target.size();
		int mid = -1;
		if (condition != null) {
			condition.compile(target, true, BreakpointType.STEP_OVER);
			mid = target.temp();
		}

		var end = new DeferredIntSupplier();

		LabelContext.pushLoop(target.env, loc(), label, end, continueTarget);
		body.compile(target, false, BreakpointType.STEP_OVER);

		continueTarget.set(target.size());
		if (assignment != null) assignment.compile(target, false, BreakpointType.STEP_OVER);
		int endI = target.size();

		end.set(endI + 1);
		LabelContext.popLoop(target.env, label);

		target.add(Instruction.jmp(start - endI));
		if (condition != null) target.set(mid, Instruction.jmpIfNot(endI - mid + 1));
		if (pollute) target.add(Instruction.pushUndefined());
	}

	public ForNode(Location loc, String label, Node declaration, Node condition, Node assignment, Node body) {
		super(loc);
		this.label = label;
		this.declaration = declaration;
		this.condition = condition;
		this.assignment = assignment;
		this.body = body;
	}

	private static ParseRes<Node> parseSemicolon(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);

		if (!src.is(i + n, ";")) return ParseRes.failed();
		else return ParseRes.res(null, n + 1);
	}
	private static ParseRes<Node> parseCondition(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);

		var res = JavaScript.parseExpression(src, i + n, 0);
		if (!res.isSuccess()) return res.chainError();
		n += res.n;
		n += Parsing.skipEmpty(src, i + n);

		if (!src.is(i + n, ";")) return ParseRes.error(src.loc(i + n), "Expected a semicolon");
		else return ParseRes.res(res.result, n + 1);
	}
	private static ParseRes<? extends Node> parseUpdater(Source src, int i) {
		return JavaScript.parseExpression(src, i, 0);
	}

	public static ParseRes<ForNode> parse(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		var labelRes = JavaScript.parseLabel(src, i + n);
		n += labelRes.n;
		n += Parsing.skipEmpty(src, i + n);

		if (!Parsing.isIdentifier(src, i + n, "for")) return ParseRes.failed();
		n += 3;
		n += Parsing.skipEmpty(src, i + n);

		if (!src.is(i + n, "(")) return ParseRes.error(src.loc(i + n), "Expected a open paren after 'for'");
		n++;

		ParseRes<Node> decl = ParseRes.first(src, i + n,
			ForNode::parseSemicolon,
			VariableDeclareNode::parse,
			ForNode::parseCondition
		);
		if (!decl.isSuccess()) return decl.chainError(src.loc(i + n), "Expected a declaration or an expression");
		n += decl.n;

		ParseRes<Node> cond = ParseRes.first(src, i + n,
			ForNode::parseSemicolon,
			ForNode::parseCondition
		);
		if (!cond.isSuccess()) return cond.chainError(src.loc(i + n), "Expected a condition");
		n += cond.n;

		var update = parseUpdater(src, i + n);
		if (update.isError()) return update.chainError();
		n += update.n;
		n += Parsing.skipEmpty(src, i + n);

		if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a close paren after for updater");
		n++;

		var body = JavaScript.parseStatement(src, i + n);
		if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected a for body.");
		n += body.n;

		return ParseRes.res(new ForNode(loc, labelRes.result, decl.result, cond.result, update.result, body.result), n);
	}
}
