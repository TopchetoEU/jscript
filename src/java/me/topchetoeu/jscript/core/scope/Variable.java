package me.topchetoeu.jscript.core.scope;

import me.topchetoeu.jscript.core.Context;

public interface Variable {
    Object get(Context ctx);
    default boolean readonly() { return true; }
    default void set(Context ctx, Object val) { }
}
