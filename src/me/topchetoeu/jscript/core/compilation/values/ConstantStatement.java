package me.topchetoeu.jscript.core.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.CompileTarget;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.compilation.Statement;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;

public class ConstantStatement extends Statement {
    public final Object value;

    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        if (pollute) target.add(Instruction.loadValue(loc(), value));
    }

    public ConstantStatement(Location loc, Object val) {
        super(loc);
        this.value = val;
    }
}
