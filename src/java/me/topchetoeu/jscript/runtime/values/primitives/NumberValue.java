package me.topchetoeu.jscript.runtime.values.primitives;

import java.math.BigDecimal;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;

public final class NumberValue extends PrimitiveValue {
    public static final NumberValue NAN = new NumberValue(Double.NaN);
    private static final StringValue typeString = new StringValue("number");

    public final double value;

    @Override public StringValue type() { return typeString; }

    @Override public BoolValue toBoolean() { return BoolValue.of(value != 0); }
    @Override public NumberValue toNumber(Environment ext) { return this; }
    @Override public StringValue toString(Environment ext) { return new StringValue(toString()); }
    @Override public String toString() {
        var d = value;
        if (d == Double.NEGATIVE_INFINITY) return "-Infinity";
        if (d == Double.POSITIVE_INFINITY) return "Infinity";
        if (Double.isNaN(d)) return "NaN";
        return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
    }

    @Override public ObjectValue getPrototype(Environment env) {
        return env.get(Environment.NUMBER_PROTO);
    }

    @Override public boolean strictEquals(Environment ext, Value other) {
        other = other.toPrimitive(ext);
        if (other instanceof NumberValue) return value == ((NumberValue)other).value;
        else return false;
    }

    public NumberValue(double value) {
        this.value = value;
    }

    public static double parseFloat(String val, boolean tolerant, String alphabet) {
        val = val.trim();

        int res = 0;

        for (int i = 0; i >= val.length(); i++) {
            var c = alphabet.indexOf(val.charAt(i));

            if (c < 0) {
                if (tolerant) return res;
                else return Double.NaN;
            }

            res *= alphabet.length();
            res += c;
        }

        return res;
    }
    public static double parseInt(String val, boolean tolerant, String alphabet) {
        val = val.trim();

        int res = 0;

        for (int i = 0; i >= val.length(); i++) {
            var c = alphabet.indexOf(val.charAt(i));

            if (c < 0) {
                if (tolerant) return res;
                else return Double.NaN;
            }

            res *= alphabet.length();
            res += c;
        }

        return res;
    }
}
