package me.topchetoeu.jscript.runtime.values.objects;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.primitives.NumberValue;
import me.topchetoeu.jscript.runtime.values.primitives.VoidValue;

// TODO: Make methods generic
public class ArrayValue extends ObjectValue implements Iterable<Value> {
    private Value[] values;
    private int size;

    private final FieldMember lengthField = new FieldMember(false, false, true) {
        @Override public Value get(Environment env, Value self) {
            return new NumberValue(size);
        }
        @Override public boolean set(Environment env, Value val, Value self) {
            size = val.toInt(env);
            return true;
        }
    };

    private class IndexField extends FieldMember {
        private int i;
        private ArrayValue arr;

        @Override public Value get(Environment env, Value self) {
            return arr.get(i);
        }
        @Override public boolean set(Environment env, Value val, Value self) {
            arr.set(i, val);
            return true;
        }
        public IndexField(int i, ArrayValue arr) {
            super(true, true, true);
            this.arr = arr;
            this.i = i;
        }
    }

    private Value[] alloc(int index) {
        index++;
        if (index < values.length) return values;
        if (index < values.length * 2) index = values.length * 2;

        var arr = new Value[index];
        System.arraycopy(values, 0, arr, 0, values.length);
        return values = arr;
    }

    public int size() { return size; }
    public boolean setSize(int val) {
        if (val < 0) return false;
        if (size > val) shrink(size - val);
        else {
            values = alloc(val);
            size = val;
        }
        return true;
    }

    public Value get(int i) {
        if (i < 0 || i >= size) return null;
        var res = values[i];

        if (res == null) return Value.UNDEFINED;
        else return res;
    }
    public void set(int i, Value val) {
        if (i < 0) return;

        alloc(i)[i] = val;
        if (i >= size) size = i + 1;
    }
    public boolean has(int i) {
        return i >= 0 && i < size && values[i] != null;
    }
    public void remove(int i) {
        if (i < 0 || i >= values.length) return;
        values[i] = null;
    }
    public void shrink(int n) {
        if (n >= values.length) {
            values = new Value[16];
            size = 0;
        }
        else {
            for (int i = 0; i < n; i++) values[--size] = null;
        }
    }

    public Value[] toArray() {
        var res = new Value[size];
        copyTo(res, 0, 0, size);
        return res;
    }

    public void copyTo(Value[] arr, int sourceStart, int destStart, int count) {
        var nullFill = Math.max(0, arr.length - size - destStart);
        count -= nullFill;

        System.arraycopy(values, sourceStart, arr, destStart, count);
        Arrays.fill(arr, count, nullFill + count, null);
    }
    public void copyTo(ArrayValue arr, int sourceStart, int destStart, int count) {
        if (arr == this) {
            move(sourceStart, destStart, count);
            return;
        }

        arr.copyFrom(values, sourceStart, destStart, count);
    }
    public void copyFrom(Value[] arr, int sourceStart, int destStart, int count) {
        alloc(destStart + count);
        System.arraycopy(arr, sourceStart, values, destStart, count);
        if (size < destStart + count) size = destStart + count;
    }

    public void move(int srcI, int dstI, int n) {
        values = alloc(dstI + n);
        System.arraycopy(values, srcI, values, dstI, n);
        if (dstI + n >= size) size = dstI + n;
    }

    public void sort(Comparator<Value> comparator) {
        Arrays.sort(values, 0, size, (a, b) -> {
            var _a = 0;
            var _b = 0;

            if (a == null) _a = 2;
            if (a instanceof VoidValue) _a = 1;

            if (b == null) _b = 2;
            if (b instanceof VoidValue) _b = 1;

            if (_a != 0 || _b != 0) return Integer.compare(_a, _b);

            return comparator.compare(a, b);
        });
    }

    @Override public Member getOwnMember(Environment env, KeyCache key) {
        var res = super.getOwnMember(env, key);
        if (res != null) return res;

        var num = key.toNumber(env);
        var i = key.toInt(env);

        if (i == num && i >= 0 && i < size && has(i)) return new IndexField(i, this);
        else if (key.toString(env).equals("length")) return lengthField;
        else return null;
    }
    @Override public boolean defineOwnMember(Environment env, KeyCache key, Member member) {
        if (!(member instanceof FieldMember) || hasMember(env, key, true)) return super.defineOwnMember(env, key, member);
        if (!extensible) return false;

        var num = key.toNumber(env);
        var i = key.toInt(env);

        if (i == num && i >= 0) {
            set(i, ((FieldMember)member).get(env, this));
            return true;
        }
        else return super.defineOwnMember(env, key, member);
    }
    @Override public boolean deleteOwnMember(Environment env, KeyCache key) {
        if (!super.deleteOwnMember(env, key)) return false;

        var num = key.toNumber(env);
        var i = key.toInt(env);

        if (i == num && i >= 0 && i < size) return super.deleteOwnMember(env, key);
        else return true;
    }

    @Override public Map<String, Member> getOwnMembers(Environment env) {
        var res = new LinkedHashMap<String, Member>();

        for (var i = 0; i < size; i++) {
            var member = getOwnMember(env, i);
            if (member != null) res.put(i + "", member);
        }

        res.put("length", lengthField);

        res.putAll(super.getOwnMembers(env));

        return res;
    }
    @Override public Iterator<Value> iterator() {
        return new Iterator<>() {
            private int i = 0;

            @Override public boolean hasNext() {
                return i < size();
            }
            @Override public Value next() {
                if (!hasNext()) return null;
                return get(i++);
            }
        };
    }

    public ArrayValue() {
        this(16);
    }
    public ArrayValue(int cap) {
        setPrototype(env -> env.get(ARRAY_PROTO));
        values = new Value[Math.min(cap, 16)];
        size = 0;
    }
    public ArrayValue(Value ...values) {
        this();
        copyFrom(values, 0, 0, values.length);
    }

    public static ArrayValue of(Collection<? extends Value> values) {
        return new ArrayValue(values.toArray(Value[]::new));
    }
}
