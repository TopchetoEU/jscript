package me.topchetoeu.jscript.utils.interop;

import java.lang.reflect.Array;

import me.topchetoeu.jscript.runtime.Context;
import me.topchetoeu.jscript.runtime.values.NativeWrapper;
import me.topchetoeu.jscript.runtime.values.Values;

public class Arguments {
    public final Object self;
    public final Object[] args;
    public final Context ctx;

    public int n() {
        return args.length;
    }

    public boolean has(int i) {
        return i == -1 || i >= 0 && i < args.length;
    }

    public <T> T self(Class<T> type) {
        return convert(-1, type);
    }
    public <T> T convert(int i, Class<T> type) {
        return Values.convert(ctx, get(i), type);
    }
    public Object get(int i, boolean unwrap) {
        Object res = null;

        if (i == -1) res = self;
        if (i >= 0 && i < args.length) res = args[i];
        if (unwrap && res instanceof NativeWrapper) res = ((NativeWrapper)res).wrapped;

        return res;
    }
    public Object get(int i) {
        return get(i, false);
    }
    public Object getOrDefault(int i, Object def) {
        if (i < 0 || i >= args.length) return def;
        else return get(i);
    }

    public Arguments slice(int start) {
        var res = new Object[Math.max(0, args.length - start)];
        for (int j = start; j < args.length; j++) res[j - start] = get(j);
        return new Arguments(ctx, args, res);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] convert(Class<T> type) {
        var res = Array.newInstance(type, args.length);
        for (int i = 0; i < args.length; i++) Array.set(res, i, convert(i, type));
        return (T[])res;
    }
    public int[] convertInt() {
        var res = new int[args.length];
        for (int i = 0; i < args.length; i++) res[i] = convert(i, Integer.class);
        return res;
    }
    public long[] convertLong() {
        var res = new long[Math.max(0, args.length)];
        for (int i = 0; i < args.length; i++) res[i] = convert(i, Long.class);
        return res;
    }
    public short[] sliceShort() {
        var res = new short[Math.max(0, args.length)];
        for (int i = 0; i < args.length; i++) res[i] = convert(i, Short.class);
        return res;
    }
    public float[] sliceFloat() {
        var res = new float[Math.max(0, args.length)];
        for (int i = 0; i < args.length; i++) res[i] = convert(i, Float.class);
        return res;
    }
    public double[] sliceDouble() {
        var res = new double[Math.max(0, args.length)];
        for (int i = 0; i < args.length; i++) res[i] = convert(i, Double.class);
        return res;
    }
    public byte[] sliceByte() {
        var res = new byte[Math.max(0, args.length)];
        for (int i = 0; i < args.length; i++) res[i] = convert(i, Byte.class);
        return res;
    }
    public char[] sliceChar() {
        var res = new char[Math.max(0, args.length)];
        for (int i = 0; i < args.length; i++) res[i] = convert(i, Character.class);
        return res;
    }
    public boolean[] sliceBool() {
        var res = new boolean[Math.max(0, args.length)];
        for (int i = 0; i < args.length; i++) res[i] = convert(i, Boolean.class);
        return res;
    }

    public String getString(int i) { return Values.toString(ctx, get(i)); }
    public boolean getBoolean(int i) { return Values.toBoolean(get(i)); }
    public int getInt(int i) { return (int)Values.toNumber(ctx, get(i)); }
    public long getLong(int i) { return (long)Values.toNumber(ctx, get(i)); }
    public double getDouble(int i) { return Values.toNumber(ctx, get(i)); }
    public float getFloat(int i) { return (float)Values.toNumber(ctx, get(i)); }

    public int getInt(int i, int def) {
        var res = get(i);
        if (res == null) return def;
        else return (int)Values.toNumber(ctx, res);
    }
    public String getString(int i, String def) {
        var res = get(i);
        if (res == null) return def;
        else return Values.toString(ctx, res);
    }

    public Arguments(Context ctx, Object thisArg, Object... args) {
        this.ctx = ctx;
        this.args = args;
        this.self = thisArg;
    }
}
