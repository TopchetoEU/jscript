package me.topchetoeu.jscript.runtime.values.primitives;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

public final class SymbolValue extends PrimitiveValue {
	private static final HashMap<String, SymbolValue> registry = new HashMap<>();

	public final String value;

	public Value key() {
		return registry.containsKey(value) && registry.get(value) == this ? StringValue.of(value) : Value.UNDEFINED;
	}

	@Override public StringValue type() { return StringValue.of("symbol"); }

	@Override public boolean toBoolean() { return false; }
	@Override public String toString(Environment env) {
		throw EngineException.ofType("Cannot convert a Symbol value to a string");
	}
	@Override public NumberValue toNumber(Environment env) {
		throw EngineException.ofType("Cannot convert a Symbol value to a number");
	}

	@Override public ObjectValue getPrototype(Environment env) { return env.get(SYMBOL_PROTO); }

	@Override public String toString() {
		if (value == null) return "Symbol()";
		else return "Symbol(" + value + ")";
	}

	@Override
	public List<String> toReadableLines(Environment env, HashSet<ObjectValue> passed) {
		return Arrays.asList(toString());
	}

	public SymbolValue(String value) {
		this.value = value;
	}

	public static SymbolValue get(String name) {
		if (registry.containsKey(name)) return registry.get(name);
		else {
			var res = new SymbolValue(name);
			registry.put(name, res);
			return res;
		}
	}
}
