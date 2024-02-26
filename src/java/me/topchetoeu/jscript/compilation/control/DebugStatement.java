package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;

public class DebugStatement extends Statement {
    @Override
    public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.debug());
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public DebugStatement(Location loc) {
        super(loc);
    }
}
