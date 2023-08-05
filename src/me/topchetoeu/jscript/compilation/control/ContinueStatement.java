package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class ContinueStatement extends Statement {
    public final String label;

    @Override
    public boolean pollutesStack() { return false; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        target.add(Instruction.nop("cont", label).locate(loc()));
    }

    public ContinueStatement(Location loc, String label) {
        super(loc);
        this.label = label;
    }
}
