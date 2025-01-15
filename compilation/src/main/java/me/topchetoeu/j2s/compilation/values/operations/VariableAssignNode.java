package me.topchetoeu.j2s.compilation.values.operations;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.common.Operation;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.FunctionNode;
import me.topchetoeu.j2s.compilation.Node;
import me.topchetoeu.j2s.compilation.values.VariableNode;

public class VariableAssignNode extends Node {
	public final String name;
	public final Node value;
	public final Operation operation;

	@Override public void compileFunctions(CompileResult target) {
		value.compileFunctions(target);
	}

	@Override public void compile(CompileResult target, boolean pollute) {
		if (operation != null) {
			target.add(VariableNode.toGet(target, loc(), name));
			FunctionNode.compileWithName(value, target, true, name);
			target.add(Instruction.operation(operation));
			target.add(VariableNode.toSet(target, loc(), name, pollute, false)).setLocation(loc());
		}
		else {
			FunctionNode.compileWithName(value, target, true, name);
			target.add(VariableNode.toSet(target, loc(), name, pollute, false)).setLocation(loc());
		}
	}

	public VariableAssignNode(Location loc, String name, Node val, Operation operation) {
		super(loc);
		this.name = name;
		this.value = val;
		this.operation = operation;
	}
}
