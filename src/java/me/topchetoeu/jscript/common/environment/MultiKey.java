package me.topchetoeu.jscript.common.environment;

import java.util.Set;

public interface MultiKey<T> extends Key<T> {
    public T of(Set<T> values);
}
