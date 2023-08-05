package me.topchetoeu.jscript.engine.scope;

import me.topchetoeu.jscript.engine.CallContext;

public interface Variable {
    Object get(CallContext ctx) throws InterruptedException;
    default boolean readonly() { return true; }
    default void set(CallContext ctx, Object val) throws InterruptedException { }
}
