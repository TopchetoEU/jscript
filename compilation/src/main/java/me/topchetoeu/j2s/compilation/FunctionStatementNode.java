package me.topchetoeu.j2s.compilation;

import java.util.List;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.common.Instruction.BreakpointType;
import me.topchetoeu.j2s.compilation.scope.Variable;
import me.topchetoeu.j2s.compilation.values.VariableNode;

public class FunctionStatementNode extends FunctionNode {
	public final String name;

	@Override public String name() { return name; }

	@Override public void resolve(CompileResult target) {
		target.scope.define(new Variable(name, false));
	}
	@Override public void compileFunctions(CompileResult target) {
		target.addChild(this, compileBody(target, name()));
	}

	@Override public void compile(CompileResult target, boolean pollute, String name, BreakpointType bp) {
		target.add(Instruction.loadFunc(target.childrenIndices.get(this), name(name), captures(target))).setLocation(loc());
		target.add(VariableNode.toSet(target, end, this.name, false, true)).setLocation(loc());
		if (pollute) target.add(Instruction.pushUndefined());
	}

	public FunctionStatementNode(Location loc, Location end, List<VariableNode> params, CompoundNode body, String name) {
		super(loc, end, params, body);
		this.name = name;
	}
}
