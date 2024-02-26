package me.topchetoeu.jscript.core;

import me.topchetoeu.jscript.core.values.Symbol;

public interface Extensions {
    <T> T get(Symbol key);
    <T> void add(Symbol key, T obj);
    Iterable<Symbol> keys();

    boolean has(Symbol key);
    boolean remove(Symbol key);

    default boolean hasNotNull(Symbol key) {
        return has(key) && get(key) != null;
    }

    default <T> T get(Symbol key, T defaultVal) {
        if (has(key)) return get(key);
        else return defaultVal;
    }

    default <T> T init(Symbol key, T val) {
        if (has(key)) return get(key);
        else {
            add(key, val);
            return val;
        }
    }
    default void addAll(Extensions source) {
        for (var key : source.keys()) {
            add(key, source.get(key));
        }
    }
}
