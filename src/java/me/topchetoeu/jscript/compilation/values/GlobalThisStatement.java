package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;

public class GlobalThisStatement extends Statement {
    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        if (pollute) target.add(Instruction.loadGlob());
    }

    public GlobalThisStatement(Location loc) {
        super(loc);
    }
}
