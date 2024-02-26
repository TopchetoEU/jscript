package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;

public class VariableIndexStatement extends Statement {
    public final int index;

    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        if (pollute) target.add(Instruction.loadVar(index));
    }

    public VariableIndexStatement(Location loc, int i) {
        super(loc);
        this.index = i;
    }
}
