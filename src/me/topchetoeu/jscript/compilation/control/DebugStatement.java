package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class DebugStatement extends Statement {
    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        target.add(Instruction.debug(loc()));
        if (pollute) target.add(Instruction.loadValue(loc(), null));
    }

    public DebugStatement(Location loc) {
        super(loc);
    }
}
