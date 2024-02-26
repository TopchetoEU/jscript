package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;

public class ThrowStatement extends Statement {
    public final Statement value;

    @Override
    public void compile(CompileResult target, boolean pollute) {
        value.compile(target, true);
        target.add(Instruction.throwInstr()).setLocation(loc());
    }

    public ThrowStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }
}
