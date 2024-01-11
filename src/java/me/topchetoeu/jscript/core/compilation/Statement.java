package me.topchetoeu.jscript.core.compilation;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;

public abstract class Statement {
    private Location _loc;

    public boolean pure() { return false; }
    public void declare(ScopeRecord varsScope) { }

    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute, BreakpointType type) {
        int start = target.size();
        compile(target, scope, pollute);

        if (target.size() != start) {
            target.get(start).locate(loc());
            target.setDebug(start, type);
        }
    }
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        compile(target, scope, pollute, BreakpointType.NONE);
    }

    public Location loc() { return _loc; }
    public void setLoc(Location loc) { _loc = loc; }

    protected Statement(Location loc) {
        this._loc = loc;
    }
}