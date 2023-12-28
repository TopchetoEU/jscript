package me.topchetoeu.jscript.interop;

import java.lang.reflect.Array;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.NativeWrapper;
import me.topchetoeu.jscript.engine.values.Values;

public class Arguments {
    public final Object thisArg;
    public final Object[] args;
    public final Context ctx;

    public <T> T get(int i, Class<T> type) {
        return Values.convert(ctx, get(i), type);
    }
    public Object get(int i, boolean unwrap) {
        Object res = null;

        if (i == -1) res = thisArg;
        if (i >= 0 && i < args.length) res = args[i];
        if (unwrap && res instanceof NativeWrapper) res = ((NativeWrapper)res).wrapped;

        return res;
    }
    public Object get(int i) {
        return get(i, false);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] slice(int i, Class<T> type) {
        var res = Array.newInstance(type, Math.max(0, args.length - i));
        for (; i < args.length; i++) Array.set(res, i - args.length, get(i, type));
        return ((T[])res);
    }
    public Object slice(int i, boolean unwrap) {
        var res = new Object[Math.max(0, args.length - i)];
        for (; i < args.length; i++) res[i - args.length] = get(i, unwrap);
        return res;
    }
    public Object slice(int i) {
        return slice(i, false);
    }

    public int[] sliceInt(int i) {
        var res = new int[Math.max(0, args.length - i)];
        for (; i < args.length; i++) res[i - args.length] = get(i, Integer.class);
        return res;
    }
    public long[] sliceLong(int i) {
        var res = new long[Math.max(0, args.length - i)];
        for (; i < args.length; i++) res[i - args.length] = get(i, Long.class);
        return res;
    }
    public short[] sliceShort(int i) {
        var res = new short[Math.max(0, args.length - i)];
        for (; i < args.length; i++) res[i - args.length] = get(i, Short.class);
        return res;
    }
    public float[] sliceFloat(int i) {
        var res = new float[Math.max(0, args.length - i)];
        for (; i < args.length; i++) res[i - args.length] = get(i, Float.class);
        return res;
    }
    public double[] sliceDouble(int i) {
        var res = new double[Math.max(0, args.length - i)];
        for (; i < args.length; i++) res[i - args.length] = get(i, Double.class);
        return res;
    }
    public byte[] sliceByte(int i) {
        var res = new byte[Math.max(0, args.length - i)];
        for (; i < args.length; i++) res[i - args.length] = get(i, Byte.class);
        return res;
    }
    public char[] sliceChar(int i) {
        var res = new char[Math.max(0, args.length - i)];
        for (; i < args.length; i++) res[i - args.length] = get(i, Character.class);
        return res;
    }
    public boolean[] sliceBool(int i) {
        var res = new boolean[Math.max(0, args.length - i)];
        for (; i < args.length; i++) res[i - args.length] = get(i, Boolean.class);
        return res;
    }

    public String getString(int i) { return Values.toString(ctx, get(i)); }
    public boolean getBoolean(int i) { return Values.toBoolean(get(i)); }
    public int getInt(int i) { return (int)Values.toNumber(ctx, get(i)); }

    public Arguments(Context ctx, Object thisArg, Object... args) {
        this.ctx = ctx;
        this.args = args;
        this.thisArg = thisArg;
    }
}
