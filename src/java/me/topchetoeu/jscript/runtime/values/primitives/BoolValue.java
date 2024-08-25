package me.topchetoeu.jscript.runtime.values.primitives;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;

public final class BoolValue extends PrimitiveValue {
    public static final BoolValue TRUE = new BoolValue(true);
    public static final BoolValue FALSE = new BoolValue(false);
    private static final StringValue typeString = new StringValue("boolean");

    public final boolean value;

    @Override public StringValue type() { return typeString; }

    @Override public BoolValue toBoolean() { return this; }
    @Override public NumberValue toNumber(Environment ext) {
        return value ? new NumberValue(1) : new NumberValue(0);
    }
    @Override public StringValue toString(Environment ext) { return new StringValue(value ? "true" : "false"); }

    @Override public ObjectValue getPrototype(Environment env) {
        return env.get(Environment.BOOL_PROTO);
    }

    @Override public boolean strictEquals(Environment ext, Value other) {
        if (other instanceof BoolValue) return value == ((BoolValue)other).value;
        else return false;
    }

    private BoolValue(boolean val) {
        this.value = val;
    }

    public static BoolValue of(boolean val) {
        return val ? TRUE : FALSE;
    }
}
