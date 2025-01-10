package me.topchetoeu.j2s.compilation.values;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.parsing.Location;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.Node;


public class ArgumentsNode extends Node {
	@Override public void compileFunctions(CompileResult target) {
	}

	@Override public void compile(CompileResult target, boolean pollute) {
		if (pollute) target.add(Instruction.loadArgs());
	}

	public ArgumentsNode(Location loc) {
		super(loc);
	}
}
