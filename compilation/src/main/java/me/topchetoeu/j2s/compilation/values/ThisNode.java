package me.topchetoeu.j2s.compilation.values;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.Node;


public class ThisNode extends Node {
	@Override public void compileFunctions(CompileResult target) {
	}

	@Override public void compile(CompileResult target, boolean pollute) {
		if (pollute) target.add(Instruction.loadThis());
	}

	public ThisNode(Location loc) {
		super(loc);
	}
}
