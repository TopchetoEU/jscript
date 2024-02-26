package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;

public class DiscardStatement extends Statement {
    public final Statement value;

    @Override public boolean pure() { return value.pure(); }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        value.compile(target, false);
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public DiscardStatement(Location loc, Statement val) {
        super(loc);
        this.value = val;
    }
}
