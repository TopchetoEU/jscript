package me.topchetoeu.jscript.runtime.scope;

import me.topchetoeu.jscript.runtime.environment.Environment;

public interface Variable {
    Object get(Environment ext);
    default boolean readonly() { return true; }
    default void set(Environment ext, Object val) { }
}
