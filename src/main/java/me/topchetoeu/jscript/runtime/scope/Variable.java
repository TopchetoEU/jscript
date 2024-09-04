package me.topchetoeu.jscript.runtime.scope;

import me.topchetoeu.jscript.runtime.Extensions;

public interface Variable {
    Object get(Extensions ext);
    default boolean readonly() { return true; }
    default void set(Extensions ext, Object val) { }
}
