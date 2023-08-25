package me.topchetoeu.jscript.compilation;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public abstract class AssignStatement extends Statement {
    public abstract void compile(List<Instruction> target, ScopeRecord scope, boolean retPrevValue);

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        compile(target, scope, false);
    }

    protected AssignStatement(Location loc) {
        super(loc);
    }
}
