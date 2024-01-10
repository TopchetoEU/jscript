package me.topchetoeu.jscript.core.engine.values;

import java.util.HashMap;

public final class Symbol {
    private static final HashMap<String, Symbol> registry = new HashMap<>();

    public final String value;

    public Symbol(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        if (value == null) return "Symbol";
        else return "@@" + value;
    }

    public static Symbol get(String name) {
        if (registry.containsKey(name)) return registry.get(name);
        else {
            var res = new Symbol(name);
            registry.put(name, res);
            return res;
        }
    }
}
