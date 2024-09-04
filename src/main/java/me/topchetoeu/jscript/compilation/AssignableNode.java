package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Operation;

public interface AssignableNode {
    public abstract Node toAssign(Node val, Operation operation);
}
