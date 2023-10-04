package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public abstract class Statement {
    private Location _loc;

    public boolean pure() { return false; }
    public abstract void compile(CompileTarget target, ScopeRecord scope, boolean pollute);
    public void declare(ScopeRecord varsScope) { }
    public Statement optimize() { return this; }

    public void compileWithDebug(CompileTarget target, ScopeRecord scope, boolean pollute) {
        int start = target.size();
        compile(target, scope, pollute);
        if (target.size() != start) target.get(start).setDebug(true);
    }

    public Location loc() { return _loc; }
    public void setLoc(Location loc) { _loc = loc; }

    protected Statement(Location loc) {
        this._loc = loc;
    }
}