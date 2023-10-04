package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class ContinueStatement extends Statement {
    public final String label;

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope, boolean pollute) {
        target.add(Instruction.nop("cont", label).locate(loc()));
        if (pollute) target.add(Instruction.loadValue(null).locate(loc()));
    }

    public ContinueStatement(Location loc, String label) {
        super(loc);
        this.label = label;
    }
}
