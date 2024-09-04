package me.topchetoeu.jscript.runtime.values;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import me.topchetoeu.jscript.runtime.environment.Environment;

// TODO: Make methods generic
public class ArrayValue extends ObjectValue implements Iterable<Object> {
    private static final Object UNDEFINED = new Object();
    private Object[] values;
    private int size;

    private Object[] alloc(int index) {
        index++;
        if (index < values.length) return values;
        if (index < values.length * 2) index = values.length * 2;

        var arr = new Object[index];
        System.arraycopy(values, 0, arr, 0, values.length);
        return arr;
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

    public Object get(int i) {
        if (i < 0 || i >= size) return null;
        var res = values[i];
        if (res == UNDEFINED) return null;
        else return res;
    }
    public void set(Environment ext, int i, Object val) {
        if (i < 0) return;

        values = alloc(i);

        val = Values.normalize(ext, val);
        if (val == null) val = UNDEFINED;
        values[i] = val;
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
    public void copyTo(ArrayValue arr, int sourceStart, int destStart, int count) {
        if (arr == this) {
            move(sourceStart, destStart, count);
            return;
        }

        // Iterate in reverse to reallocate at most once
        if (destStart + count > arr.size) arr.size = destStart + count;

        for (var i = count - 1; i >= 0; i--) {
            if (i + sourceStart < 0 || i + sourceStart >= size) arr.remove(i + destStart);
            if (values[i + sourceStart] == UNDEFINED) arr.set(null, i + destStart, null);
            else if (values[i + sourceStart] == null) arr.remove(i + destStart);
            else arr.set(null, i + destStart, values[i + sourceStart]);
        }
    }

    public void copyFrom(Environment ext, Object[] arr, int sourceStart, int destStart, int count) {
        for (var i = 0; i < count; i++) {
            set(ext, i + destStart, arr[i + sourceStart]);
        }
    }

    public void move(int srcI, int dstI, int n) {
        values = alloc(dstI + n);

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
    protected Object getField(Environment ext, Object key) {
        if (key instanceof Number) {
            var i = ((Number)key).doubleValue();
            if (i >= 0 && i - Math.floor(i) == 0) {
                return get((int)i);
            }
        }

        return super.getField(ext, key);
    }
    @Override
    protected boolean setField(Environment ext, Object key, Object val) {
        if (key instanceof Number) {
            var i = Values.number(key);
            if (i >= 0 && i - Math.floor(i) == 0) {
                set(ext, (int)i, val);
                return true;
            }
        }

        return super.setField(ext, key, val);
    }
    @Override
    protected boolean hasField(Environment ext, Object key) {
        if (key instanceof Number) {
            var i = Values.number(key);
            if (i >= 0 && i - Math.floor(i) == 0) {
                return has((int)i);
            }
        }

        return super.hasField(ext, key);
    }
    @Override
    protected void deleteField(Environment ext, Object key) {
        if (key instanceof Number) {
            var i = Values.number(key);
            if (i >= 0 && i - Math.floor(i) == 0) {
                remove((int)i);
                return;
            }
        }

        super.deleteField(ext, key);
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
    public ArrayValue(Environment ext, Object ...values) {
        this();
        this.values = new Object[values.length];
        size = values.length;

        for (var i = 0; i < size; i++) this.values[i] = Values.normalize(ext, values[i]);
    }

    public static ArrayValue of(Environment ext, Collection<?> values) {
        return new ArrayValue(ext, values.toArray(Object[]::new));
    }
}
