package me.topchetoeu.jscript.runtime.values.objects;

import java.util.Comparator;
import java.util.Iterator;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.primitives.NumberValue;

// TODO: Make methods generic
public class ByteBufferValue extends ArrayLikeValue implements Iterable<Value> {
    public final byte[] values;

    // private Value[] alloc(int index) {
    //     index++;
    //     if (index < values.length) return values;
    //     if (index < values.length * 2) index = values.length * 2;

    //     var arr = new Value[index];
    //     System.arraycopy(values, 0, arr, 0, values.length);
    //     return values = arr;
    // }

    public int size() { return values.length; }
    public boolean setSize(int val) { return false; }

    @Override public Value get(int i) {
        if (i < 0 || i >= values.length) return null;
        return new NumberValue(values[i]);
    }
    @Override public boolean set(Environment env, int i, Value val) {
        if (i < 0 || i >= values.length) return false;
        values[i] = (byte)val.toNumber(env).value;
        return true;
    }
    @Override public boolean has(int i) {
        return i >= 0 && i < values.length;
    }
    @Override public boolean remove(int i) {
        return false;
    }

    public void copyTo(byte[] arr, int sourceStart, int destStart, int count) {
        System.arraycopy(values, sourceStart, arr, destStart, count);
    }
    public void copyTo(ByteBufferValue arr, int sourceStart, int destStart, int count) {
        arr.copyFrom(values, sourceStart, destStart, count);
    }
    public void copyFrom(byte[] arr, int sourceStart, int destStart, int count) {
        System.arraycopy(arr, sourceStart, arr, destStart, count);
    }

    public void move(int srcI, int dstI, int n) {
        System.arraycopy(values, srcI, values, dstI, n);
    }

    public void sort(Comparator<Value> comparator) {
        throw new RuntimeException("not supported");
        // Arrays.sort(values, 0, values.length, (a, b) -> {
        //     var _a = 0;
        //     var _b = 0;

        //     if (a == null) _a = 2;
        //     if (a instanceof VoidValue) _a = 1;

        //     if (b == null) _b = 2;
        //     if (b instanceof VoidValue) _b = 1;

        //     if (_a != 0 || _b != 0) return Integer.compare(_a, _b);

        //     return comparator.compare(a, b);
        // });
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

    public ByteBufferValue(int size) {
        this(new byte[size]);
    }
    public ByteBufferValue(byte[] buffer) {
        setPrototype(BYTE_BUFF_PROTO);
        this.values = buffer;
    }
}
