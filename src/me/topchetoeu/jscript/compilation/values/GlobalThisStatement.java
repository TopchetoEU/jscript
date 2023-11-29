package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class GlobalThisStatement extends Statement {
    @Override
    public boolean pure() { return true; }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        if (pollute) target.add(Instruction.loadGlob(loc()));
    }

    public GlobalThisStatement(Location loc) {
        super(loc);
    }
}
