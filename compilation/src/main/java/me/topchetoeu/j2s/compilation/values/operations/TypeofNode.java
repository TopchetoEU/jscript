package me.topchetoeu.j2s.compilation.values.operations;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.JavaScript;
import me.topchetoeu.j2s.compilation.Node;
import me.topchetoeu.j2s.compilation.parsing.ParseRes;
import me.topchetoeu.j2s.compilation.parsing.Parsing;
import me.topchetoeu.j2s.compilation.parsing.Source;
import me.topchetoeu.j2s.compilation.values.VariableNode;

public class TypeofNode extends Node {
	public final Node value;

	@Override public void compileFunctions(CompileResult target) {
		value.compileFunctions(target);
	}

	@Override public void compile(CompileResult target, boolean pollute) {
		if (value instanceof VariableNode varNode) {
			target.add(VariableNode.toGet(target, varNode.loc(), varNode.name, true, true));
			if (pollute) target.add(Instruction.typeof());
			else target.add(Instruction.discard());

			return;
		}

		value.compile(target, pollute);
		if (pollute) target.add(Instruction.typeof());
	}

	public TypeofNode(Location loc, Node value) {
		super(loc);
		this.value = value;
	}

	public static ParseRes<TypeofNode> parse(Source src, int i) {
		var n = Parsing.skipEmpty(src, i);
		var loc = src.loc(i + n);

		if (!Parsing.isIdentifier(src, i + n, "typeof")) return ParseRes.failed();
		n += 6;

		var valRes = JavaScript.parseExpression(src, i + n, 15);
		if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'typeof' keyword.");
		n += valRes.n;
	
		return ParseRes.res(new TypeofNode(loc, valRes.result), n);
	}
}
