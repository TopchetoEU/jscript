package me.topchetoeu.jscript.runtime.values;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.SymbolValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

public final class KeyCache {
    public final Value value;
    private boolean isInt;
    private int intCache;
    private Double doubleCache;
    private Boolean booleanCache;
    private String stringCache;

    public String toString(Environment env) {
        if (stringCache != null) return stringCache;
        else return stringCache = value.toString(env);
    }
    public double toNumber(Environment env) {
        if (doubleCache == null) {
            var res = value.toNumber(env);
            isInt = res.isInt();
            intCache = res.getInt();
            doubleCache = res.getDouble();
        }

        return doubleCache;
    }
    public boolean isInt(Environment env) {
        if (doubleCache == null) toNumber(env);
        return isInt;
    }
    public int toInt(Environment env) {
        if (doubleCache == null) toNumber(env);
        return intCache;
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
        this.value = NumberValue.of(value);
        this.intCache = value;
        this.doubleCache = (double)value;
        this.booleanCache = value != 0;
    }
    public KeyCache(double value) {
        this.value = NumberValue.of(value);
        this.intCache = (int)value;
        this.doubleCache = value;
        this.booleanCache = value != 0;
    }
}
