package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;

public abstract class Node {
    private Location loc;

    public void resolve(CompileResult target) {}

    public void compile(CompileResult target, boolean pollute, BreakpointType type) {
        int start = target.size();
        compile(target, pollute);
        if (target.size() != start) target.setLocationAndDebug(start, loc(), type);
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