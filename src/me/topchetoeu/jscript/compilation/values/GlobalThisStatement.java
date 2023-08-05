package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class GlobalThisStatement extends Statement {
    @Override
    public boolean pollutesStack() { return true; }
    @Override
    public boolean pure() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        target.add(Instruction.loadGlob().locate(loc()));
    }

    public GlobalThisStatement(Location loc) {
        super(loc);
    }
}
