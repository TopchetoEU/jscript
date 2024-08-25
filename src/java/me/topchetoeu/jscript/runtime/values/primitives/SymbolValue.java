package me.topchetoeu.jscript.runtime.values.primitives;

import java.util.HashMap;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;

public final class SymbolValue extends PrimitiveValue {
    private static final HashMap<String, SymbolValue> registry = new HashMap<>();
    private static final StringValue typeString = new StringValue("symbol");

    public final String value;

    @Override public StringValue type() { return typeString; }

    @Override public BoolValue toBoolean() { return BoolValue.TRUE; }
    @Override public StringValue toString(Environment env) {
        return new StringValue(toString());
    }
    @Override public NumberValue toNumber(Environment env) {
        throw EngineException.ofType("Can't convert symbol to number");
    }

    @Override public boolean strictEquals(Environment ext, Value other) {
        return other == this;
    }
    @Override public ObjectValue getPrototype(Environment env) { return env.get(Environment.SYMBOL_PROTO); }

    @Override public String toString() {
        if (value == null) return "Symbol";
        else return "@@" + value;
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
