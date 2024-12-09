package me.topchetoeu.jscript.runtime.values.primitives;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

public final class UserValue<T> extends Value {
    public final T value;
	public final ObjectValue prototype;

    @Override public StringValue type() { return StringValue.of("object"); }

    @Override public boolean toBoolean() { return true; }
    @Override public NumberValue toNumber(Environment ext) { return NumberValue.NAN; }
    @Override public String toString(Environment ext) { return "[user value]"; }

    @Override public final boolean defineOwnMember(Environment env, KeyCache key, Member member) { return false; }
    @Override public final boolean deleteOwnMember(Environment env, KeyCache key) { return false; }
    @Override public final boolean isPrimitive() { return false; }
    @Override public final Value toPrimitive(Environment env) { return NumberValue.NAN; }

    @Override public final boolean setPrototype(Environment env, ObjectValue val) { return false; }

    @Override public Member getOwnMember(Environment env, KeyCache key) { return null; }
    @Override public Set<String> getOwnMembers(Environment env, boolean onlyEnumerable) { return new HashSet<>(); }
    @Override public Set<SymbolValue> getOwnSymbolMembers(Environment env, boolean onlyEnumerable) { return new HashSet<>(); }

    @Override public State getState() { return State.FROZEN; }

    @Override public void preventExtensions() {}
    @Override public void seal() {}
    @Override public void freeze() {}

	@Override public int hashCode() {
		return value.hashCode();
	}
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        else if (other instanceof UserValue val) return Objects.equals(value, val.value);
        else return false;
    }

    @Override public ObjectValue getPrototype(Environment env) { return prototype; }

    @Override public List<String> toReadableLines(Environment env, HashSet<ObjectValue> passed) {
        return Arrays.asList(value.toString());
    }

    private UserValue(T value, ObjectValue prototype) {
        this.value = value;
		this.prototype = prototype;
    }

    public static <T> UserValue<T> of(T value) {
		return new UserValue<T>(value, null);
    }
    public static <T> UserValue<T> of(T value, ObjectValue prototype) {
		return new UserValue<T>(value, prototype);
    }
}
