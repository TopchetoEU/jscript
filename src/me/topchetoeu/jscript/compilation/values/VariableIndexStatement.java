package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class VariableIndexStatement extends Statement {
    public final int index;

    @Override
    public boolean pure() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope, boolean pollute) {
        if (pollute) target.add(Instruction.loadVar(index).locate(loc()));
    }

    public VariableIndexStatement(Location loc, int i) {
        super(loc);
        this.index = i;
    }
}
