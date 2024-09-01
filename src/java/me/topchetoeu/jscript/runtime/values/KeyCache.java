package me.topchetoeu.jscript.runtime.values;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.primitives.NumberValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.SymbolValue;

public final class KeyCache {
    public final Value value;
    private Integer intCache;
    private Double doubleCache;
    private Boolean booleanCache;
    private String stringCache;

    public String toString(Environment env) {
        if (stringCache != null) return stringCache;
        else return stringCache = value.toString(env).value;
    }
    public double toNumber(Environment env) {
        if (doubleCache != null) return doubleCache;
        else return doubleCache = value.toNumber(env).value;
    }
    public int toInt(Environment env) {
        if (intCache != null) return intCache;
        else return intCache = (int)toNumber(env);
    }
    public boolean toBoolean() {
        if (booleanCache != null) return booleanCache;
        else return booleanCache = value.toBoolean();
    }
    public SymbolValue toSymbol() {
        if (value instanceof SymbolValue) return (SymbolValue)value;
        else return null;
    }
    public boolean isSymbol() {
        return value instanceof SymbolValue;
    }

    public KeyCache(Value value) {
        this.value = value;
    }
    public KeyCache(String value) {
        this.value = new StringValue(value);
        this.stringCache = value;
        this.booleanCache = !value.equals("");
    }
    public KeyCache(int value) {
        this.value = new NumberValue(value);
        this.intCache = value;
        this.doubleCache = (double)value;
        this.booleanCache = value != 0;
    }
    public KeyCache(double value) {
        this.value = new NumberValue(value);
        this.intCache = (int)value;
        this.doubleCache = value;
        this.booleanCache = value != 0;
    }
}
