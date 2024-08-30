package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Operation;

public interface AssignableStatement {
    public abstract Statement toAssign(Statement val, Operation operation);
}
