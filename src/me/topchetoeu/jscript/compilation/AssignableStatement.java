package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction.Type;

public abstract class AssignableStatement extends Statement {
    public abstract Statement toAssign(Statement val, Type operation);

    protected AssignableStatement(Location loc) {
        super(loc);
    }
}
