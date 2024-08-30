package me.topchetoeu.jscript.runtime.values.objects;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.primitives.NumberValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.SymbolValue;

public class ObjectValue extends Value {
    public static interface PrototypeProvider {
        public ObjectValue get(Environment env);
    }

    public static enum State {
        NORMAL,
        NO_EXTENSIONS,
        SEALED,
        FROZEN,
    }

    public static class Property { 
        public final FunctionValue getter;
        public final FunctionValue setter;

        public Property(FunctionValue getter, FunctionValue setter) {
            this.getter = getter;
            this.setter = setter;
        }
    }

    private static final StringValue typeString = new StringValue("object");

    protected PrototypeProvider prototype;

    public boolean extensible = true;

    public LinkedHashMap<String, Member> members = new LinkedHashMap<>();
    public LinkedHashMap<SymbolValue, Member> symbolMembers = new LinkedHashMap<>();

    @Override public boolean isPrimitive() { return false; }
    @Override public Value toPrimitive(Environment env) {
        if (env != null) {
            var valueOf = getMember(env, new StringValue("valueOf"));

            if (valueOf instanceof FunctionValue) {
                var res = valueOf.call(env, this);
                if (res.isPrimitive()) return res;
            }

            var toString = getMember(env, new StringValue("toString"));
            if (toString instanceof FunctionValue) {
                var res = toString.call(env, this);
                if (res.isPrimitive()) return res;
            }
        }

        throw EngineException.ofType("Value couldn't be converted to a primitive.");
    }
    @Override public StringValue toString(Environment env) { return toPrimitive(env).toString(env); }
    @Override public boolean toBoolean() { return true; }
    @Override public NumberValue toNumber(Environment env) { return toPrimitive(env).toNumber(env);  }
    @Override public StringValue type() { return typeString; }

    @Override public boolean strictEquals(Environment ext, Value other) { return this == other; }

    public final void preventExtensions() {
        extensible = false;
    }

    @Override public Member getOwnMember(Environment env, KeyCache key) {
        if (key.isSymbol()) return symbolMembers.get(key.toSymbol());
        else return members.get(key.toString(env));
    }
    @Override public boolean defineOwnMember(Environment env, KeyCache key, Member member) {
        var old = getOwnMember(env, key);
        if (old != null && old.configure(env, member, this)) return true;
        if (old != null && !old.configurable()) return false;

        if (key.isSymbol()) symbolMembers.put(key.toSymbol(), member);
        else members.put(key.toString(env), member);

        return true;
    }
    @Override public boolean deleteOwnMember(Environment env, KeyCache key) {
        if (!extensible) return false;

        var member = getOwnMember(env, key);
        if (member == null) return true;
        if (member.configurable()) return false;

        if (key.isSymbol()) symbolMembers.remove(key.toSymbol());
        else members.remove(key.toString(env));
        return true;
    }

    @Override public Map<String, Member> getOwnMembers(Environment env) {
        return members;
    }
    @Override public Map<SymbolValue, Member> getOwnSymbolMembers(Environment env) {
        return Collections.unmodifiableMap(symbolMembers);
    }

    @Override public ObjectValue getPrototype(Environment env) {
        if (prototype == null || env == null) return null;
        else return prototype.get(env);
    }
    @Override public final boolean setPrototype(Environment env, ObjectValue val) {
        return setPrototype(_env -> val);
    }

    public final boolean setPrototype(PrototypeProvider val) {
        if (!extensible) return false;
        prototype = val;
        return true;
    }
    public final boolean setPrototype(Key<ObjectValue> key) {
        if (!extensible) return false;
        prototype = env -> env.get(key);
        return true;
    }
}
