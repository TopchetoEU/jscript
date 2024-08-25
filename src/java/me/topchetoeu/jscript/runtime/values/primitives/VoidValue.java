package me.topchetoeu.jscript.runtime.values.primitives;

import java.util.Map;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;

public final class VoidValue extends PrimitiveValue {
    public static final VoidValue UNDEFINED = new VoidValue("undefined", new StringValue("undefined"));
    public static final VoidValue NULL = new VoidValue("null", new StringValue("null"));

    private final StringValue namestring;

    public final String name;
    public final StringValue typeString;

    @Override public StringValue type() { return typeString; }
    @Override public BoolValue toBoolean() { return BoolValue.FALSE; }
    @Override public NumberValue toNumber(Environment ext) { return NumberValue.NAN; }
    @Override public StringValue toString(Environment ext) { return namestring; }

    @Override public Value add(Environment ext, Value other) {
        if (!other.isPrimitive()) other = other.toPrimitive(ext);

        if (other instanceof StringValue) return namestring.add(ext, other);
        else return NumberValue.NAN;
    }

    @Override public boolean strictEquals(Environment ext, Value other) {
        return this == other;
    }
    @Override public ObjectValue getPrototype(Environment env) { return null; }

    @Override public Member getOwnMember(Environment env, Value key) {
        throw EngineException.ofError(String.format("Cannot read properties of %s (reading %s)", name, key.toString(env).value));
    }
    @Override public Map<String, Member> getOwnMembers(Environment env) {
        throw EngineException.ofError(String.format("Cannot read properties of %s (listing all members)", name));
    }
    @Override public Map<SymbolValue, Member> getOwnSymbolMembers(Environment env) {
        throw EngineException.ofError(String.format("Cannot read properties of %s (listing all symbol members)", name));
    }

    public VoidValue(String name, StringValue type) {
        this.name = name;
        this.typeString = type;
        this.namestring = new StringValue(name);
    }
}
