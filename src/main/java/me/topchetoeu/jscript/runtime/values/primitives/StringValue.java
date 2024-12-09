package me.topchetoeu.jscript.runtime.values.primitives;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.json.JSON;
import me.topchetoeu.jscript.common.json.JSONElement;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

public final class StringValue extends PrimitiveValue {
    private static final WeakHashMap<String, StringValue> cache = new WeakHashMap<>();

    public final String value;

    @Override public StringValue type() { return of("string"); }

    @Override public boolean toBoolean() { return !value.equals(""); }
    @Override public NumberValue toNumber(Environment ext) {
        var val = value.trim();
        if (val.equals("")) return NumberValue.of(0);
        var res = Parsing.parseNumber(new Source(val), 0, true);

        if (res.isSuccess() && res.n == val.length()) return NumberValue.of(res.result);
        else return NumberValue.NAN;
    }
    @Override public String toString(Environment ext) { return value; }

	@Override public int hashCode() {
		return value.hashCode();
	}
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        else if (other instanceof StringValue val) return value.length() == val.value.length() && value.equals(val.value);
        else return false;
    }

    @Override public ObjectValue getPrototype(Environment env) { return env.get(STRING_PROTO); }

    @Override public Member getOwnMember(Environment env, KeyCache key) {
        if (!key.isSymbol()) {
            var num = key.toNumber(env);
            var i = key.toInt(env);

            if (i == num && i >= 0 && i < value.length()) {
                return FieldMember.of(this, new StringValue(value.charAt(i) + ""), false, true, false);
            }
            else if (key.toString(env).equals("length")) {
                return FieldMember.of(this, NumberValue.of(value.length()), false, false, false);
            }
        }

        return super.getOwnMember(env, key);
    }

    @Override public Set<String> getOwnMembers(Environment env, boolean onlyEnumerable) {
        var res = new LinkedHashSet<String>();

        res.addAll(super.getOwnMembers(env, onlyEnumerable));

        for (var i = 0; i < value.length(); i++) res.add(i + "");

        if (!onlyEnumerable) res.add("length");

        return res;
    }

    @Override public List<String> toReadableLines(Environment env, HashSet<ObjectValue> passed) {
        return Arrays.asList(JSON.stringify(JSONElement.string(value)));
    }

    private StringValue(String value) {
        this.value = value;
    }

    public static StringValue of(String value) {
        if (cache.containsKey(value)) return cache.get(value);
        else {
            StringValue res;
            cache.put(value, res = new StringValue(value));
            return res;
        }
    }
}
