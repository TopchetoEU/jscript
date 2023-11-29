package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

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
