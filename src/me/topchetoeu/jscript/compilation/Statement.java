package me.topchetoeu.jscript.compilation;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public abstract class Statement {
    private Location _loc;

    public abstract boolean pollutesStack();
    public boolean pure() { return false; }
    public abstract void compile(List<Instruction> target, ScopeRecord scope);
    public void declare(ScopeRecord varsScope) { }
    public Statement optimize() { return this; }

    public void compileNoPollution(List<Instruction> target, ScopeRecord scope, boolean debug) {
        int start = target.size();
        compile(target, scope);
        if (debug && target.size() != start) target.get(start).setDebug(true);
        if (pollutesStack()) target.add(Instruction.discard().locate(loc()));
    }
    public void compileWithPollution(List<Instruction> target, ScopeRecord scope, boolean debug) {
        int start = target.size();
        compile(target, scope);
        if (debug && target.size() != start) target.get(start).setDebug(true);
        if (!pollutesStack()) target.add(Instruction.loadValue(null).locate(loc()));
    }
    public void compileNoPollution(List<Instruction> target, ScopeRecord scope) {
        compileNoPollution(target, scope, false);
    }
    public void compileWithPollution(List<Instruction> target, ScopeRecord scope) {
        compileWithPollution(target, scope, false);
    }

    public Location loc() { return _loc; }
    public void setLoc(Location loc) { _loc = loc; }

    protected Statement(Location loc) {
        this._loc = loc;
    }
}