package me.topchetoeu.jscript.engine;

import me.topchetoeu.jscript.engine.values.Symbol;

public abstract class ExtensionStack implements Extensions {
    protected abstract Extensions[] extensionStack();

    @Override public <T> void add(Symbol key, T obj) {
        for (var el : extensionStack()) {
            if (el != null) {
                el.add(key, obj);
                return;
            }
        }
    }
    @Override public <T> T get(Symbol key) {
        for (var el : extensionStack()) {
            if (el != null && el.has(key)) return el.get(key);
        }

        return null;
    }
    @Override public boolean has(Symbol key) {
        for (var el : extensionStack()) {
            if (el != null && el.has(key)) return true;
        }

        return false;
    }
    @Override public boolean remove(Symbol key) {
        var anyRemoved = false;

        for (var el : extensionStack()) {
            if (el != null) anyRemoved &= el.remove(key);
        }

        return anyRemoved;
    }
}
