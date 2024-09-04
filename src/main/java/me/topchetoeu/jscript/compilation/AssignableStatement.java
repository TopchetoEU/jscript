package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Operation;

public abstract class AssignableStatement extends Statement {
    public abstract Statement toAssign(Statement val, Operation operation);

    protected AssignableStatement(Location loc) {
        super(loc);
    }
}
