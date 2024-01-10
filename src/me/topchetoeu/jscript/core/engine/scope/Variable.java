package me.topchetoeu.jscript.core.engine.scope;

import me.topchetoeu.jscript.core.engine.Context;

public interface Variable {
    Object get(Context ctx);
    default boolean readonly() { return true; }
    default void set(Context ctx, Object val) { }
}
