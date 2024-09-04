package me.topchetoeu.jscript.runtime.values.primitives;

import java.util.Map;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;

public final class StringValue extends PrimitiveValue {
    public final String value;
    private static final StringValue typeString = new StringValue("string");

    @Override public StringValue type() { return typeString; }

    @Override public boolean toBoolean() { return !value.equals(""); }
    @Override public NumberValue toNumber(Environment ext) {
        var val = value.trim();
        if (val.equals("")) return new NumberValue(0);
        var res = Parsing.parseNumber(new Source(val), 0, true);

        if (res.isSuccess() && res.n == val.length()) return new NumberValue(res.result);
        else return new NumberValue(Double.NaN);
    }
    @Override public StringValue toString(Environment ext) { return this; }

    @Override public boolean equals(Object other) {
        if (other instanceof StringValue val) return value.length() == val.value.length() && value.equals(val.value);
        else return false;
    }

    @Override public ObjectValue getPrototype(Environment env) { return env.get(STRING_PROTO); }

    @Override public Map<String, Member> getOwnMembers(Environment env) {
        // TODO Auto-generated method stub
        return super.getOwnMembers(env);
    }

    public StringValue(String value) {
        this.value = value;
    }
}
