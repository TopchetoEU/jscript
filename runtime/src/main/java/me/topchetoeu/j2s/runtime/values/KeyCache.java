package me.topchetoeu.j2s.runtime.values;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.runtime.values.primitives.StringValue;
import me.topchetoeu.j2s.runtime.values.primitives.SymbolValue;
import me.topchetoeu.j2s.runtime.values.primitives.numbers.NumberValue;

// Not really a cache, more of a semantic correction patchwork
// If we have a proto-chain of objects, all of which need to convert the given key to a given type, this will do it exactly once
// This in turn will prevent calling .toString() on an object more than once, thus keeping consistent semantics
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
		this.value = StringValue.of(value);
		this.stringCache = value;
		this.booleanCache = !value.equals("");
	}
	public KeyCache(int value) {
		this.value = NumberValue.of(value);
		this.isInt = true;
		this.intCache = value;
		this.doubleCache = (double)value;
		this.booleanCache = value != 0;
	}
	public KeyCache(double value) {
		this.value = NumberValue.of(value);
		this.isInt = (int)value == value;
		this.intCache = (int)value;
		this.doubleCache = value;
		this.booleanCache = value != 0;
	}
}
