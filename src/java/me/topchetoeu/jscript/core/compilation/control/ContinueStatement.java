package me.topchetoeu.jscript.core.compilation.control;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.CompileTarget;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.compilation.Statement;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;

public class ContinueStatement extends Statement {
    public final String label;

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        target.add(Instruction.nop(loc(), "cont", label));
        if (pollute) target.add(Instruction.loadValue(loc(), null));
    }

    public ContinueStatement(Location loc, String label) {
        super(loc);
        this.label = label;
    }
}
