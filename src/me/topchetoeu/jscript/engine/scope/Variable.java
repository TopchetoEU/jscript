package me.topchetoeu.jscript.engine.scope;

import me.topchetoeu.jscript.engine.Context;

public interface Variable {
    Object get(Context ctx) throws InterruptedException;
    default boolean readonly() { return true; }
    default void set(Context ctx, Object val) throws InterruptedException { }
}
