package me.topchetoeu.jscript.compilation.values.constants;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;

public class NullStatement extends Statement {
    @Override public boolean pure() { return true; }

    @Override public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.pushNull());
    }

    public NullStatement(Location loc) { super(loc); }
}
