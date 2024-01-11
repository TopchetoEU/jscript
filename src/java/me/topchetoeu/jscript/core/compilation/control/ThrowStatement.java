package me.topchetoeu.jscript.core.compilation.control;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.CompileTarget;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.compilation.Statement;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;

public class ThrowStatement extends Statement {
    public final Statement value;

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        value.compile(target, scope, true);
        target.add(Instruction.throwInstr(loc()));
    }

    public ThrowStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }
}
