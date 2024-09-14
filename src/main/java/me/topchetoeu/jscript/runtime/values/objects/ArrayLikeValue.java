package me.topchetoeu.jscript.runtime.values.objects;

import java.util.LinkedHashSet;
import java.util.Set;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;
import me.topchetoeu.jscript.runtime.values.Value;

public abstract class ArrayLikeValue extends ObjectValue {
    private static class IndexField extends FieldMember {
        private int i;
        private ArrayLikeValue arr;

        @Override public Value get(Environment env, Value self) {
            return arr.get(i);
        }
        @Override public boolean set(Environment env, Value val, Value self) {
            return arr.set(env, i, val);
        }
        public IndexField(int i, ArrayLikeValue arr) {
            super(arr, true, true, true);
            this.arr = arr;
            this.i = i;
        }
    }

    private final FieldMember lengthField = new FieldMember(this, false, false, true) {
        @Override public Value get(Environment env, Value self) {
            return NumberValue.of(size());
        }
        @Override public boolean set(Environment env, Value val, Value self) {
            var num = val.toNumber(env);
            if (!num.isInt()) throw EngineException.ofRange("Invalid array length");

            var i = num.getInt();
            if (i < 0) throw EngineException.ofRange("Invalid array length");

            return setSize(i);
        }
    };

    public abstract int size();
    public abstract boolean setSize(int val);

    public abstract Value get(int i);
    public abstract boolean set(Environment env, int i, Value val);
    public abstract boolean has(int i);
    public abstract boolean remove(int i);

    @Override public Member getOwnMember(Environment env, KeyCache key) {
        var res = super.getOwnMember(env, key);
        if (res != null) return res;

        var num = key.toNumber(env);
        var i = key.toInt(env);

        if (i == num && i >= 0 && i < size() && has(i)) return new IndexField(i, this);
        else if (key.toString(env).equals("length")) return lengthField;
        else return null;
    }
    @Override public boolean defineOwnMember(Environment env, KeyCache key, Member member) {
        if (!(member instanceof FieldMember) || super.getOwnMember(env, key) != null) return super.defineOwnMember(env, key, member);
        if (!getState().writable) return false;

        var num = key.toNumber(env);
        var i = key.toInt(env);

        if (i == num) {
            if (!getState().extendable && !has(i)) return false;
            if (set(env, i, ((FieldMember)member).get(env, this))) return true;
        }

        return super.defineOwnMember(env, key, member);
    }
    @Override public boolean deleteOwnMember(Environment env, KeyCache key) {
        if (!super.deleteOwnMember(env, key)) return false;

        var num = key.toNumber(env);
        var i = key.toInt(env);

        if (i == num && i >= 0 && i < size()) return remove(i);
        else return true;
    }

    @Override public Set<String> getOwnMembers(Environment env, boolean onlyEnumerable) {
        var res = new LinkedHashSet<String>();

        res.addAll(super.getOwnMembers(env, onlyEnumerable));

        for (var i = 0; i < size(); i++) {
            if (has(i)) res.add(i + "");
        }

        if (!onlyEnumerable) res.add("length");

        return res;
    }
}
