package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;

public abstract class Statement {
    private Location _loc;

    public boolean pure() { return false; }
    public void declare(CompileResult target) { }

    public void compile(CompileResult target, boolean pollute, BreakpointType type) {
        int start = target.size();
        compile(target, pollute);
        if (target.size() != start) target.setLocationAndDebug(start, loc(), type);
    }
    public void compile(CompileResult target, boolean pollute) {
        compile(target, pollute, BreakpointType.NONE);
    }

    public Location loc() { return _loc; }
    public void setLoc(Location loc) { _loc = loc; }

    protected Statement(Location loc) {
        this._loc = loc;
    }
}