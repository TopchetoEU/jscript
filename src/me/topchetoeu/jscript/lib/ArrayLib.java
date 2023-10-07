package me.topchetoeu.jscript.lib;

import java.util.Iterator;
import java.util.Stack;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.InitType;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeConstructor;
import me.topchetoeu.jscript.interop.NativeGetter;
import me.topchetoeu.jscript.interop.NativeInit;
import me.topchetoeu.jscript.interop.NativeSetter;

public class ArrayLib {
    @NativeGetter(thisArg = true) public static int length(Context ctx, ArrayValue thisArg) {
        return thisArg.size();
    }
    @NativeSetter(thisArg = true) public static void length(Context ctx, ArrayValue thisArg, int len) {
        thisArg.setSize(len);
    }
    
    @Native(thisArg = true) public static ObjectValue values(Context ctx, ArrayValue thisArg) {
        return Values.fromJavaIterable(ctx, thisArg);
    }
    @Native(thisArg = true) public static ObjectValue keys(Context ctx, ArrayValue thisArg) {
        return Values.fromJavaIterable(ctx, () -> new Iterator<Object>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < thisArg.size();
            }
            @Override
            public Object next() {
                if (!hasNext()) return null;
                return i++;
            }
        });
    }
    @Native(thisArg = true) public static ObjectValue entries(Context ctx, ArrayValue thisArg) {
        return Values.fromJavaIterable(ctx, () -> new Iterator<Object>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < thisArg.size();
            }
            @Override
            public Object next() {
                if (!hasNext()) return null;
                return new ArrayValue(ctx, i, thisArg.get(i++));
            }
        });
    }

    @Native(value = "@@Symbol.iterator", thisArg = true)
    public static ObjectValue iterator(Context ctx, ArrayValue thisArg) {
        return values(ctx, thisArg);
    }
    @Native(value = "@@Symbol.asyncIterator", thisArg = true)
    public static ObjectValue asyncIterator(Context ctx, ArrayValue thisArg) {
        return values(ctx, thisArg);
    }

    @Native(thisArg = true) public static ArrayValue concat(Context ctx, ArrayValue thisArg, Object ...others) {
        // TODO: Fully implement with non-array spreadable objects
        var size = 0;

        for (int i = 0; i < others.length; i++) {
            if (others[i] instanceof ArrayValue) size += ((ArrayValue)others[i]).size();
            else i++;
        }

        var res = new ArrayValue(size);

        for (int i = 0, j = 0; i < others.length; i++) {
            if (others[i] instanceof ArrayValue) {
                int n = ((ArrayValue)others[i]).size();
                ((ArrayValue)others[i]).copyTo(ctx, res, 0, j, n);
                j += n;
            }
            else {
                res.set(ctx, j++, others[i]);
            }
        }

        return res;
    }

    @Native(thisArg = true) public static void sort(Context ctx, ArrayValue arr, FunctionValue cmp) {
        arr.sort((a, b) -> {
            var res = Values.toNumber(ctx, cmp.call(ctx, null, a, b));
            if (res < 0) return -1;
            if (res > 0) return 1;
            return 0;
        });
    }

    private static int normalizeI(int len, int i, boolean clamp) {
        if (i < 0) i += len;
        if (clamp) {
            if (i < 0) i = 0;
            if (i >= len) i = len;
        }
        return i;
    }

    @Native(thisArg = true) public static ArrayValue fill(Context ctx, ArrayValue arr, Object val, int start, int end) {
        start = normalizeI(arr.size(), start, true);
        end = normalizeI(arr.size(), end, true);

        for (; start < end; start++) {
            arr.set(ctx, start, val);
        }

        return arr;
    }
    @Native(thisArg = true) public static ArrayValue fill(Context ctx, ArrayValue arr, Object val, int start) {
        return fill(ctx, arr, val, start, arr.size());
    }
    @Native(thisArg = true) public static ArrayValue fill(Context ctx, ArrayValue arr, Object val) {
        return fill(ctx, arr, val, 0, arr.size());
    }

    @Native(thisArg = true) public static boolean every(Context ctx, ArrayValue arr, FunctionValue func, Object thisArg) {
        for (var i = 0; i < arr.size(); i++) {
            if (!Values.toBoolean(func.call(ctx, thisArg, arr.get(i), i, arr))) return false;
        }

        return true;
    }
    @Native(thisArg = true) public static boolean some(Context ctx, ArrayValue arr, FunctionValue func, Object thisArg) {
        for (var i = 0; i < arr.size(); i++) {
            if (Values.toBoolean(func.call(ctx, thisArg, arr.get(i), i, arr))) return true;
        }

        return false;
    }

    @Native(thisArg = true) public static ArrayValue filter(Context ctx, ArrayValue arr, FunctionValue func, Object thisArg) {
        var res = new ArrayValue(arr.size());

        for (int i = 0, j = 0; i < arr.size(); i++) {
            if (arr.has(i) && Values.toBoolean(func.call(ctx, thisArg, arr.get(i), i, arr))) res.set(ctx, j++, arr.get(i));
        }
        return res;
    }
    @Native(thisArg = true) public static ArrayValue map(Context ctx, ArrayValue arr, FunctionValue func, Object thisArg) {
        var res = new ArrayValue(arr.size());
        for (int i = 0, j = 0; i < arr.size(); i++) {
            if (arr.has(i)) res.set(ctx, j++, func.call(ctx, thisArg, arr.get(i), i, arr));
        }
        return res;
    }
    @Native(thisArg = true) public static void forEach(Context ctx, ArrayValue arr, FunctionValue func, Object thisArg) {
        for (int i = 0; i < arr.size(); i++) {
            if (arr.has(i)) func.call(ctx, thisArg, arr.get(i), i, arr);
        }
    }

    @Native(thisArg = true) public static ArrayValue flat(Context ctx, ArrayValue arr, int depth) {
        var res = new ArrayValue(arr.size());
        var stack = new Stack<Object>();
        var depths = new Stack<Integer>();

        stack.push(arr);
        depths.push(-1);

        while (!stack.empty()) {
            var el = stack.pop();
            int d = depths.pop();

            if (d <= depth && el instanceof ArrayValue) {
                for (int i = ((ArrayValue)el).size() - 1; i >= 0; i--) {
                    stack.push(((ArrayValue)el).get(i));
                    depths.push(d + 1);
                }
            }
            else res.set(ctx, depth, arr);
        }

        return res;
    }
    @Native(thisArg = true) public static ArrayValue flatMap(Context ctx, ArrayValue arr, FunctionValue cmp, Object thisArg) {
        return flat(ctx, map(ctx, arr, cmp, thisArg), 1);
    }

    @Native(thisArg = true) public static Object find(Context ctx, ArrayValue arr, FunctionValue cmp, Object thisArg) {
        for (int i = 0; i < arr.size(); i++) {
            if (arr.has(i) && Values.toBoolean(cmp.call(ctx, thisArg, arr.get(i), i, arr))) return arr.get(i);
        }

        return null;
    }
    @Native(thisArg = true) public static Object findLast(Context ctx, ArrayValue arr, FunctionValue cmp, Object thisArg) {
        for (var i = arr.size() - 1; i >= 0; i--) {
            if (arr.has(i) && Values.toBoolean(cmp.call(ctx, thisArg, arr.get(i), i, arr))) return arr.get(i);
        }

        return null;
    }

    @Native(thisArg = true) public static int findIndex(Context ctx, ArrayValue arr, FunctionValue cmp, Object thisArg) {
        for (int i = 0; i < arr.size(); i++) {
            if (arr.has(i) && Values.toBoolean(cmp.call(ctx, thisArg, arr.get(i), i, arr))) return i;
        }

        return -1;
    }
    @Native(thisArg = true) public static int findLastIndex(Context ctx, ArrayValue arr, FunctionValue cmp, Object thisArg) {
        for (var i = arr.size() - 1; i >= 0; i--) {
            if (arr.has(i) && Values.toBoolean(cmp.call(ctx, thisArg, arr.get(i), i, arr))) return i;
        }

        return -1;
    }

    @Native(thisArg = true) public static int indexOf(Context ctx, ArrayValue arr, Object val, int start) {
        start = normalizeI(arr.size(), start, true);

        for (int i = 0; i < arr.size() && i < start; i++) {
            if (Values.strictEquals(ctx, arr.get(i), val)) return i;
        }

        return -1;
    }
    @Native(thisArg = true) public static int lastIndexOf(Context ctx, ArrayValue arr, Object val, int start) {
        start = normalizeI(arr.size(), start, true);

        for (int i = arr.size(); i >= start; i--) {
            if (Values.strictEquals(ctx, arr.get(i), val)) return i;
        }

        return -1;
    }

    @Native(thisArg = true) public static boolean includes(Context ctx, ArrayValue arr, Object el, int start) {
        return indexOf(ctx, arr, el, start) >= 0;
    }

    @Native(thisArg = true) public static Object pop(Context ctx, ArrayValue arr) {
        if (arr.size() == 0) return null;
        var val = arr.get(arr.size() - 1);
        arr.shrink(1);
        return val;
    }
    @Native(thisArg = true) public static int push(Context ctx, ArrayValue arr, Object ...values) {
        arr.copyFrom(ctx, values, 0, arr.size(), values.length);
        return arr.size();
    }

    @Native(thisArg = true) public static Object shift(Context ctx, ArrayValue arr) {
        if (arr.size() == 0) return null;
        var val = arr.get(0);
        arr.move(1, 0, arr.size());
        arr.shrink(1);
        return val;
    }
    @Native(thisArg = true) public static int unshift(Context ctx, ArrayValue arr, Object ...values) {
        arr.move(0, values.length, arr.size());
        arr.copyFrom(ctx, values, 0, 0, values.length);
        return arr.size();
    }

    @Native(thisArg = true) public static ArrayValue slice(Context ctx, ArrayValue arr, int start, int end) {
        start = normalizeI(arr.size(), start, true);
        end = normalizeI(arr.size(), end, true);

        var res = new ArrayValue(end - start);
        arr.copyTo(ctx, res, start, 0, end - start);
        return res;
    }
    @Native(thisArg = true) public static ArrayValue slice(Context ctx, ArrayValue arr, int start) {
        return slice(ctx, arr, start, arr.size());
    }

    @Native(thisArg = true) public static ArrayValue splice(Context ctx, ArrayValue arr, int start, int deleteCount, Object ...items) {
        start = normalizeI(arr.size(), start, true);
        deleteCount = normalizeI(arr.size(), deleteCount, true);
        if (start + deleteCount >= arr.size()) deleteCount = arr.size() - start;

        var size = arr.size() - deleteCount + items.length;
        var res = new ArrayValue(deleteCount);
        arr.copyTo(ctx, res, start, 0, deleteCount);
        arr.move(start + deleteCount, start + items.length, arr.size() - start - deleteCount);
        arr.copyFrom(ctx, items, 0, start, items.length);
        arr.setSize(size);

        return res;
    }
    @Native(thisArg = true) public static ArrayValue splice(Context ctx, ArrayValue arr, int start) {
        return splice(ctx, arr, start, arr.size() - start);
    }
    @Native(thisArg = true) public static String toString(Context ctx, ArrayValue arr) {
        return join(ctx, arr, ",");
    }

    @Native(thisArg = true) public static String join(Context ctx, ArrayValue arr, String sep) {
        var res = new StringBuilder();
        var comma = true;

        for (int i = 0; i < arr.size(); i++) {
            if (!arr.has(i)) continue;
            if (comma) res.append(sep);
            comma = false;
            var el = arr.get(i);
            if (el == null || el == Values.NULL) continue;

            res.append(Values.toString(ctx, el));
        }

        return res.toString();
    }

    @Native public static boolean isArray(Context ctx, Object val) { return val instanceof ArrayValue; }
    @Native public static ArrayValue of(Context ctx, Object... args) {
        var res = new ArrayValue(args.length);
        res.copyFrom(ctx, args, 0, 0, args.length);
        return res;
    }

    @NativeConstructor public static ArrayValue constructor(Context ctx, Object... args) {
        ArrayValue res;

        if (args.length == 1 && args[0] instanceof Number) {
            int len = ((Number)args[0]).intValue();
            res = new ArrayValue(len);
            res.setSize(len);
        }
        else {
            res = new ArrayValue(args.length);
            res.copyFrom(ctx, args, 0, 0, args.length);
        }

        return res;
    }

    @NativeInit(InitType.PROTOTYPE) public static void init(Environment env, ObjectValue target) {
        target.defineProperty(null, env.symbol("Symbol.typeName"), "Array");
    }
}
