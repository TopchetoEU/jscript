package me.topchetoeu.jscript.compilation.values.constants;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;

public class BoolStatement extends Statement {
    public final boolean value;

    @Override public boolean pure() { return true; }

    @Override public void compile(CompileResult target, boolean pollute) {
        if (pollute) target.add(Instruction.pushValue(value));
    }

    public BoolStatement(Location loc, boolean value) {
        super(loc);
        this.value = value;
    }
}
