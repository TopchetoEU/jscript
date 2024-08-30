package me.topchetoeu.jscript.runtime.values.primitives;

import me.topchetoeu.jscript.common.json.JSON;
import me.topchetoeu.jscript.common.json.JSONElement;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;

public final class NumberValue extends PrimitiveValue {
    public static final NumberValue NAN = new NumberValue(Double.NaN);
    private static final StringValue typeString = new StringValue("number");

    public final double value;

    @Override public StringValue type() { return typeString; }

    @Override public boolean toBoolean() { return value != 0; }
    @Override public NumberValue toNumber(Environment ext) { return this; }
    @Override public StringValue toString(Environment ext) { return new StringValue(toString()); }
    @Override public String toString() { return JSON.stringify(JSONElement.number(value)); }

    @Override public ObjectValue getPrototype(Environment env) {
        return env.get(Environment.NUMBER_PROTO);
    }

    @Override public CompareResult compare(Environment env, Value other) {
        if (other instanceof NumberValue) return CompareResult.from(Double.compare(value, ((NumberValue)other).value));
        else return super.compare(env, other);
    }
    @Override public boolean strictEquals(Environment ext, Value other) {
        other = other.toPrimitive(ext);
        if (other instanceof NumberValue) return value == ((NumberValue)other).value;
        else return false;
    }

    public NumberValue(double value) {
        this.value = value;
    }

    public static NumberValue parseInt(String str, int radix, boolean relaxed) {
        if (radix < 2 || radix > 36) return new NumberValue(Double.NaN);

        str = str.trim();
        var res = Parsing.parseInt(new Source(null, str), 0, "0123456789abcdefghijklmnopqrstuvwxyz".substring(0, radix), true);
        if (res.isSuccess()) {
            if (relaxed || res.n == str.length()) return new NumberValue(res.result);
        }
        return new NumberValue(Double.NaN);
    }
    public static NumberValue parseFloat(String str, boolean relaxed) {
        str = str.trim();
        var res = Parsing.parseFloat(new Source(null, str), 0, true);
        if (res.isSuccess()) {
            if (relaxed || res.n == str.length()) return new NumberValue(res.result);
        }
        return new NumberValue(Double.NaN);
    }
}
