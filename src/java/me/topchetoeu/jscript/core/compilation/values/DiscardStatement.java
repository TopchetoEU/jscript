package me.topchetoeu.jscript.core.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.CompileTarget;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.compilation.Statement;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;

public class DiscardStatement extends Statement {
    public final Statement value;

    @Override public boolean pure() { return value.pure(); }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        value.compile(target, scope, false);
        if (pollute) target.add(Instruction.loadValue(loc(), null));
    }

    public DiscardStatement(Location loc, Statement val) {
        super(loc);
        this.value = val;
    }
}
