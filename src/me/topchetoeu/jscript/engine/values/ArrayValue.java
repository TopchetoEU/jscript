package me.topchetoeu.jscript.engine.values;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import me.topchetoeu.jscript.engine.Context;

// TODO: Make methods generic
public class ArrayValue extends ObjectValue implements Iterable<Object> {
    private static final Object UNDEFINED = new Object();
    private Object[] values;
    private int size;

    private void alloc(int index) {
        if (index < values.length) return;
        if (index < values.length * 2) index = values.length * 2;

        var arr = new Object[index];
        System.arraycopy(values, 0, arr, 0, values.length);
        values = arr;
    }

    public int size() { return size; }
    public boolean setSize(int val) {
        if (val < 0) return false;
        if (size > val) shrink(size - val);
        else {
            alloc(val);
            size = val;
        }
        return true;
    }

    public Object get(int i) {
        if (i < 0 || i >= size) return null;
        var res = values[i];
        if (res == UNDEFINED) return null;
        else return res;
    }
    public void set(Context ctx, int i, Object val) {
        if (i < 0) return;

        alloc(i);

        val = Values.normalize(ctx, val);
        if (val == null) val = UNDEFINED;
        values[i] = val;
        if (i >= size) size = i + 1;
    }
    public boolean has(int i) {
        return i >= 0 && i < values.length && values[i] != null;
    }
    public void remove(int i) {
        if (i < 0 || i >= values.length) return;
        values[i] = null;
    }
    public void shrink(int n) {
        if (n >= values.length) {
            values = new Object[16];
            size = 0;
        }
        else {
            for (int i = 0; i < n; i++) {
                values[--size] = null;
            }
        }
    }

    public Object[] toArray() {
        Object[] res = new Object[size];
        copyTo(res, 0, 0, size);
        return res;
    }
    public void copyTo(Object[] arr, int sourceStart, int destStart, int count) {
        for (var i = 0; i < count; i++) {
            if (i + sourceStart < 0 || i + sourceStart >= size) arr[i + destStart] = null;
            if (values[i + sourceStart] == UNDEFINED) arr[i + destStart] = null;
            else arr[i + sourceStart] = values[i + destStart];
        }
    }
    public void copyTo(Context ctx, ArrayValue arr, int sourceStart, int destStart, int count) {
        // Iterate in reverse to reallocate at most once
        for (var i = count - 1; i >= 0; i--) {
            if (i + sourceStart < 0 || i + sourceStart >= size) arr.set(ctx, i + destStart, null);
            if (values[i + sourceStart] == UNDEFINED) arr.set(ctx, i + destStart, null);
            else arr.set(ctx, i + destStart, values[i + sourceStart]);
        }
    }

    public void copyFrom(Context ctx, Object[] arr, int sourceStart, int destStart, int count) {
        for (var i = 0; i < count; i++) {
            set(ctx, i + destStart, arr[i + sourceStart]);
        }
    }

    public void move(int srcI, int dstI, int n) {
        alloc(dstI + n);

        System.arraycopy(values, srcI, values, dstI, n);

        if (dstI + n >= size) size = dstI + n;
    }

    public void sort(Comparator<Object> comparator) {
        Arrays.sort(values, 0, size, (a, b) -> {
            var _a = 0;
            var _b = 0;

            if (a == UNDEFINED) _a = 1;
            if (a == null) _a = 2;

            if (b == UNDEFINED) _b = 1;
            if (b == null) _b = 2;

            if (_a != 0 || _b != 0) return Integer.compare(_a, _b);

            return comparator.compare(a, b);
        });
    }

    @Override
    protected Object getField(Context ctx, Object key) throws InterruptedException {
        if (key instanceof Number) {
            var i = ((Number)key).doubleValue();
            if (i >= 0 && i - Math.floor(i) == 0) {
                return get((int)i);
            }
        }

        return super.getField(ctx, key);
    }
    @Override
    protected boolean setField(Context ctx, Object key, Object val) throws InterruptedException {
        if (key instanceof Number) {
            var i = Values.number(key);
            if (i >= 0 && i - Math.floor(i) == 0) {
                set(ctx, (int)i, val);
                return true;
            }
        }

        return super.setField(ctx, key, val);
    }
    @Override
    protected boolean hasField(Context ctx, Object key) throws InterruptedException {
        if (key instanceof Number) {
            var i = Values.number(key);
            if (i >= 0 && i - Math.floor(i) == 0) {
                return has((int)i);
            }
        }

        return super.hasField(ctx, key);
    }
    @Override
    protected void deleteField(Context ctx, Object key) throws InterruptedException {
        if (key instanceof Number) {
            var i = Values.number(key);
            if (i >= 0 && i - Math.floor(i) == 0) {
                remove((int)i);
                return;
            }
        }

        super.deleteField(ctx, key);
    }

    @Override
    public List<Object> keys(boolean includeNonEnumerable) {
        var res = super.keys(includeNonEnumerable);
        for (var i = 0; i < size(); i++) {
            if (has(i)) res.add(i);
        }
        return res;
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < size();
            }
            @Override
            public Object next() {
                if (!hasNext()) return null;
                return get(i++);
            }
        };
    }

    public ArrayValue() {
        super(PlaceholderProto.ARRAY);
        values = new Object[16];
        size = 0;
    }
    public ArrayValue(int cap) {
        super(PlaceholderProto.ARRAY);
        values = new Object[cap];
        size = 0;
    }
    public ArrayValue(Context ctx, Object ...values) {
        this();
        values = new Object[values.length];
        size = values.length;

        for (var i = 0; i < size; i++) this.values[i] = Values.normalize(ctx, values[i]);
    }

    public static ArrayValue of(Context ctx, Collection<Object> values) {
        return new ArrayValue(ctx, values.toArray(Object[]::new));
    }
}
