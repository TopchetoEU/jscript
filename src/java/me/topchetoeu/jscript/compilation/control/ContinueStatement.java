package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;

public class ContinueStatement extends Statement {
    public final String label;

    @Override
    public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.nop(loc(), "cont", label));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public ContinueStatement(Location loc, String label) {
        super(loc);
        this.label = label;
    }
}
