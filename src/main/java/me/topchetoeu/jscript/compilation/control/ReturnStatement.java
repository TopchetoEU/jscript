package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;

public class ReturnStatement extends Statement {
    public final Statement value;

    @Override
    public void compile(CompileResult target, boolean pollute) {
        if (value == null) target.add(Instruction.pushUndefined());
        else value.compile(target, true);
        target.add(Instruction.ret()).setLocation(loc());
    }

    public ReturnStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }
}
