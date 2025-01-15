package me.topchetoeu.j2s.compilation.values.constants;

import me.topchetoeu.j2s.common.Instruction;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.compilation.CompileResult;
import me.topchetoeu.j2s.compilation.Node;

public class BoolNode extends Node {
	public final boolean value;

	@Override public void compileFunctions(CompileResult target) {
	}

	@Override public void compile(CompileResult target, boolean pollute) {
		if (pollute) target.add(Instruction.pushValue(value));
	}

	public BoolNode(Location loc, boolean value) {
		super(loc);
		this.value = value;
	}
}
