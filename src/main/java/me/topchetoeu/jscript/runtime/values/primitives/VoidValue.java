package me.topchetoeu.jscript.runtime.values.primitives;

import java.util.Set;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;

public final class VoidValue extends PrimitiveValue {
    private final StringValue nameString;

    public final String name;
    public final StringValue typeString;

    @Override public StringValue type() { return typeString; }
    @Override public boolean toBoolean() { return false; }
    @Override public NumberValue toNumber(Environment ext) { return NumberValue.NAN; }
    @Override public StringValue toString(Environment ext) { return nameString; }

    @Override public ObjectValue getPrototype(Environment env) { return null; }

    @Override public Member getOwnMember(Environment env, KeyCache key) {
        throw EngineException.ofError(String.format("Cannot read properties of %s (reading '%s')", name, key.toString(env)));
    }
    @Override public Set<String> getOwnMembers(Environment env, boolean onlyEnumerable) {
        throw EngineException.ofError(String.format("Cannot read properties of %s (listing all members)", name));
    }
    @Override public Set<SymbolValue> getOwnSymbolMembers(Environment env, boolean onlyEnumerable) {
        throw EngineException.ofError(String.format("Cannot read properties of %s (listing all symbol members)", name));
    }

    public VoidValue(String name, StringValue type) {
        this.name = name;
        this.typeString = type;
        this.nameString = new StringValue(name);
    }
}
