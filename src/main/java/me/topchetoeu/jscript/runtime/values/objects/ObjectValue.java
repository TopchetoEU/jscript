package me.topchetoeu.jscript.runtime.values.objects;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.Member.PropertyMember;
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

    protected PrototypeProvider prototype;

    public LinkedHashMap<String, Member> members = new LinkedHashMap<>();
    public LinkedHashMap<SymbolValue, Member> symbolMembers = new LinkedHashMap<>();

    @Override public boolean isPrimitive() { return false; }
    @Override public Value toPrimitive(Environment env) {
        if (env != null) {
            var valueOf = getMember(env, "valueOf");

            if (valueOf instanceof FunctionValue) {
                var res = valueOf.apply(env, this);
                if (res.isPrimitive()) return res;
            }

            var toString = getMember(env, "toString");
            if (toString instanceof FunctionValue) {
                var res = toString.apply(env, this);
                if (res.isPrimitive()) return res;
            }
        }

        throw EngineException.ofType("Value couldn't be converted to a primitive.");
    }
    @Override public String toString(Environment env) { return toPrimitive(env).toString(env); }
    @Override public boolean toBoolean() { return true; }
    @Override public NumberValue toNumber(Environment env) { return toPrimitive(env).toNumber(env);  }
    @Override public StringValue type() { return StringValue.of("object"); }

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
        if (key.isSymbol()) {
            if (symbolMembers.size() > 0) return symbolMembers.get(key.toSymbol());
            else return null;
        }
        else if (members.size() > 0) return members.get(key.toString(env));
        else return null;
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

    private final LinkedList<String> memberToReadable(Environment env, String key, Member member, HashSet<ObjectValue> passed) {
        if (member instanceof PropertyMember prop) {
            if (prop.getter == null && prop.setter == null) return new LinkedList<>(Arrays.asList(key + ": [No accessors]"));
            else if (prop.getter == null) return new LinkedList<>(Arrays.asList(key + ": [Setter]"));
            else if (prop.setter == null) return new LinkedList<>(Arrays.asList(key + ": [Getter]"));
            else return new LinkedList<>(Arrays.asList(key + ": [Getter/Setter]"));
        }
        else {
            var res = new LinkedList<String>();
            var first = true;

            for (var line : member.get(env, this).toReadableLines(env, passed)) {
                if (first) res.add(key + ": " + line);
                else res.add(line);
                first = false;
            }

            return res;
        }
    }

    public List<String> toReadableLines(Environment env, HashSet<ObjectValue> passed, HashSet<String> ignoredKeys) {
        passed.add(this);

        var stringified = new LinkedList<LinkedList<String>>();

        for (var entry : getOwnSymbolMembers(env, true)) {
            var member = getOwnMember(env, entry);
            stringified.add(memberToReadable(env, "[" + entry.value + "]", member, passed));
        }
        for (var entry : getOwnMembers(env, true)) {
            if (ignoredKeys.contains(entry)) continue;

            var member = getOwnMember(env, entry);
            stringified.add(memberToReadable(env, entry, member, passed));
        }

        passed.remove(this);

        if (stringified.size() == 0) return Arrays.asList("{}");
        var concat = new StringBuilder();
        for (var entry : stringified) {
            // We make a one-liner only when all members are one-liners
            if (entry.size() != 1) {
                concat = null;
                break;
            }

            if (concat.length() != 0) concat.append(", ");
            concat.append(entry.get(0));
        }

        // We don't want too long one-liners
        if (concat != null && concat.length() < 80) return Arrays.asList("{ " + concat.toString() + " }");

        var res = new LinkedList<String>();

        res.add("{");

        for (var entry : stringified) {
            for (var line : entry) {
                res.add("    " + line);
            }

            res.set(res.size() - 1, res.getLast() + ",");
        }
        res.set(res.size() - 1, res.getLast().substring(0, res.getLast().length() - 1));
        res.add("}");

        return res;
    }
    @Override public List<String> toReadableLines(Environment env, HashSet<ObjectValue> passed) {
        return toReadableLines(env, passed, new HashSet<>());
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
