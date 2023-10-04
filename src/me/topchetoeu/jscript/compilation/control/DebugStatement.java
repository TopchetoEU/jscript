package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class DebugStatement extends Statement {
    @Override
    public void compile(List<Instruction> target, ScopeRecord scope, boolean pollute) {
        target.add(Instruction.debug().locate(loc()));
        if (pollute) target.add(Instruction.loadValue(null).locate(loc()));
    }

    public DebugStatement(Location loc) {
        super(loc);
    }
}
