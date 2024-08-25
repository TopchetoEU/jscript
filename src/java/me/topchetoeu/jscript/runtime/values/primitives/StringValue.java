package me.topchetoeu.jscript.runtime.values.primitives;

import java.util.Objects;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;

public final class StringValue extends PrimitiveValue {
    public final String value;
    private static final StringValue typeString = new StringValue("string");

    @Override public StringValue type() { return typeString; }

    @Override public BoolValue toBoolean() { return BoolValue.of(!value.equals("")); }
    @Override public NumberValue toNumber(Environment ext) {
        try { return new NumberValue(Double.parseDouble(value)); }
        catch (NumberFormatException e) { return new NumberValue(Double.NaN); }
    }
    @Override public StringValue toString(Environment ext) { return this; }

    @Override public Value add(Environment ext, Value other) {
        return new StringValue(value + other.toString(ext).value);
    }

    @Override public boolean strictEquals(Environment ext, Value other) {
        return (other instanceof StringValue) && Objects.equals(((StringValue)other).value, value);
    }
    @Override public ObjectValue getPrototype(Environment env) { return env.get(Environment.STRING_PROTO); }

    public StringValue(String value) {
        this.value = value;
    }
}
