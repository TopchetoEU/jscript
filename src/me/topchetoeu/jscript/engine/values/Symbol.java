package me.topchetoeu.jscript.engine.values;

public final class Symbol {
    public final String value;

    public Symbol(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        if (value == null) return "Symbol";
        else return "@@" + value;
    }
}
