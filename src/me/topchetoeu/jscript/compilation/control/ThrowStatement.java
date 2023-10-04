package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class ThrowStatement extends Statement {
    public final Statement value;

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope, boolean pollute) {
        value.compile(target, scope, true);
        target.add(Instruction.throwInstr().locate(loc()));
    }

    public ThrowStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }
}
