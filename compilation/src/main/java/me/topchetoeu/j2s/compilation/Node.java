package me.topchetoeu.j2s.compilation;

import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.common.Instruction.BreakpointType;

public abstract class Node {
	private Location loc;

	public void resolve(CompileResult target) {}

	public void compile(CompileResult target, boolean pollute, BreakpointType type) {
		compileStatement(target, pollute, type);
	}
	public void compileStatement(CompileResult target, boolean pollute, BreakpointType type) {
		int start = target.size();
		compile(target, pollute);
		if (target.size() != start) {
			target.setLocationAndDebug(start, loc(), type);
		}
	}
	public void compile(CompileResult target, boolean pollute) {
		compile(target, pollute, BreakpointType.NONE);
	}

	public abstract void compileFunctions(CompileResult target);

	public Location loc() { return loc; }
	public void setLoc(Location loc) { this.loc = loc; }

	protected Node(Location loc) {
		this.loc = loc;
	}
}
