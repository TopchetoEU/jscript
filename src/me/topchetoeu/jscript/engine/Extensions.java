package me.topchetoeu.jscript.engine;

import me.topchetoeu.jscript.engine.values.Symbol;

public interface Extensions {
    <T> T get(Symbol key);
    <T> void add(Symbol key, T obj);

    boolean has(Symbol key);
    boolean remove(Symbol key);

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
}
