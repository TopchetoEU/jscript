package me.topchetoeu.jscript.runtime.values.primitives;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;

public final class BoolValue extends PrimitiveValue {
    public static final BoolValue TRUE = new BoolValue(true);
    public static final BoolValue FALSE = new BoolValue(false);
    private static final StringValue typeString = new StringValue("boolean");

    public final boolean value;

    @Override public StringValue type() { return typeString; }

    @Override public boolean toBoolean() { return value; }
    @Override public NumberValue toNumber(Environment ext) {
        return value ? new NumberValue(1) : new NumberValue(0);
    }
    @Override public StringValue toString(Environment ext) { return new StringValue(value ? "true" : "false"); }

    @Override public ObjectValue getPrototype(Environment env) {
        return env.get(BOOL_PROTO);
    }

    @Override public boolean equals(Object other) {
        if (other instanceof BoolValue bool) return value == bool.value;
        else return false;
    }

    private BoolValue(boolean val) {
        this.value = val;
    }

    public static BoolValue of(boolean val) {
        return val ? TRUE : FALSE;
    }
}
