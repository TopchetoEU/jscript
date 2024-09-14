package me.topchetoeu.jscript.runtime.values.objects;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.SymbolValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

public class ObjectValue extends Value {
    public static interface PrototypeProvider {
        public ObjectValue get(Environment env);
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

    public LinkedHashMap<String, Member> members = new LinkedHashMap<>();
    public LinkedHashMap<SymbolValue, Member> symbolMembers = new LinkedHashMap<>();

    @Override public boolean isPrimitive() { return false; }
    @Override public Value toPrimitive(Environment env) {
        if (env != null) {
            var valueOf = getMember(env, new StringValue("valueOf"));

            if (valueOf instanceof FunctionValue) {
                var res = valueOf.invoke(env, this);
                if (res.isPrimitive()) return res;
            }

            var toString = getMember(env, new StringValue("toString"));
            if (toString instanceof FunctionValue) {
                var res = toString.invoke(env, this);
                if (res.isPrimitive()) return res;
            }
        }

        throw EngineException.ofType("Value couldn't be converted to a primitive.");
    }
    @Override public String toString(Environment env) { return toPrimitive(env).toString(env); }
    @Override public boolean toBoolean() { return true; }
    @Override public NumberValue toNumber(Environment env) { return toPrimitive(env).toNumber(env);  }
    @Override public StringValue type() { return typeString; }

    private State state = State.NORMAL;

    @Override public State getState() { return state; }

    public final void preventExtensions() {
        if (state == State.NORMAL) state = State.NON_EXTENDABLE;
    }
    public final void seal() {
        if (state == State.NORMAL || state == State.NON_EXTENDABLE) state = State.SEALED;
    }
    @Override public final void freeze() { state = State.FROZEN; }

    @Override public Member getOwnMember(Environment env, KeyCache key) {
        if (key.isSymbol()) return symbolMembers.get(key.toSymbol());
        else return members.get(key.toString(env));
    }
    @Override public boolean defineOwnMember(Environment env, KeyCache key, Member member) {
        var old = getOwnMember(env, key);
        if (old != null && old.redefine(env, member, this)) return true;
        if (old != null && !old.configurable()) return false;

        if (key.isSymbol()) symbolMembers.put(key.toSymbol(), member);
        else members.put(key.toString(env), member);

        return true;
    }
    @Override public boolean deleteOwnMember(Environment env, KeyCache key) {
        if (!getState().extendable) return false;

        var member = getOwnMember(env, key);
        if (member == null) return true;
        if (!member.configurable()) return false;

        if (key.isSymbol()) symbolMembers.remove(key.toSymbol());
        else members.remove(key.toString(env));
        return true;
    }

    @Override public Set<String> getOwnMembers(Environment env, boolean onlyEnumerable) {
        if (onlyEnumerable) {
            var res = new LinkedHashSet<String>();

            for (var el : members.entrySet()) {
                if (el.getValue().enumerable()) res.add(el.getKey());
            }

            return res;
        }
        else  return members.keySet();
    }
    @Override public Set<SymbolValue> getOwnSymbolMembers(Environment env, boolean onlyEnumerable) {
        if (onlyEnumerable) {
            var res = new LinkedHashSet<SymbolValue>();

            for (var el : symbolMembers.entrySet()) {
                if (el.getValue().enumerable()) res.add(el.getKey());
            }

            return res;
        }
        else  return symbolMembers.keySet();
    }

    @Override public ObjectValue getPrototype(Environment env) {
        if (prototype == null || env == null) return null;
        else return prototype.get(env);
    }
    @Override public final boolean setPrototype(Environment env, ObjectValue val) {
        return setPrototype(_env -> val);
    }

    public final boolean setPrototype(PrototypeProvider val) {
        if (!getState().extendable) return false;
        prototype = val;
        return true;
    }
    public final boolean setPrototype(Key<ObjectValue> key) {
        if (!getState().extendable) return false;
        prototype = env -> env.get(key);
        return true;
    }
}
