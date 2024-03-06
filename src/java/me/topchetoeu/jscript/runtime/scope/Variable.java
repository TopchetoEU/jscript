package me.topchetoeu.jscript.runtime.scope;

import me.topchetoeu.jscript.runtime.Context;

public interface Variable {
    Object get(Context ctx);
    default boolean readonly() { return true; }
    default void set(Context ctx, Object val) { }
}
