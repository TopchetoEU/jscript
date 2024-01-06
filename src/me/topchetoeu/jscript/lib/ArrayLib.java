package me.topchetoeu.jscript.lib;

import java.util.Iterator;
import java.util.Stack;

import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.Expose;
import me.topchetoeu.jscript.interop.ExposeConstructor;
import me.topchetoeu.jscript.interop.ExposeTarget;
import me.topchetoeu.jscript.interop.ExposeType;
import me.topchetoeu.jscript.interop.WrapperName;

@WrapperName("Array")
public class ArrayLib {
    private static int normalizeI(int len, int i, boolean clamp) {
        if (i < 0) i += len;
        if (clamp) {
            if (i < 0) i = 0;
            if (i > len) i = len;
        }
        return i;
    }

    @Expose(value = "length", type = ExposeType.GETTER)
    public static int __getLength(Arguments args) {
        return args.self(ArrayValue.class).size();
    }
    @Expose(value = "length", type = ExposeType.SETTER)
    public static void __setLength(Arguments args) {
        args.self(ArrayValue.class).setSize(args.getInt(0));
    }

    @Expose public static ObjectValue __values(Arguments args) {
        return __iterator(args);
    }
    @Expose public static ObjectValue __keys(Arguments args) {
        return Values.toJSIterator(args.ctx, () -> new Iterator<Object>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < args.self(ArrayValue.class).size();
            }
            @Override
            public Object next() {
                if (!hasNext()) return null;
                return i++;
            }
        });
    }
    @Expose public static ObjectValue __entries(Arguments args) {
        return Values.toJSIterator(args.ctx, () -> new Iterator<Object>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < args.self(ArrayValue.class).size();
            }
            @Override
            public Object next() {
                if (!hasNext()) return null;
                return new ArrayValue(args.ctx, i, args.self(ArrayValue.class).get(i++));
            }
        });
    }

    @Expose(value = "@@Symbol.iterator")
    public static ObjectValue __iterator(Arguments args) {
        return Values.toJSIterator(args.ctx, args.self(ArrayValue.class));
    }
    @Expose(value = "@@Symbol.asyncIterator")
    public static ObjectValue __asyncIterator(Arguments args) {
        return Values.toJSAsyncIterator(args.ctx, args.self(ArrayValue.class).iterator());
    }

    @Expose public static ArrayValue __concat(Arguments args) {
        // TODO: Fully implement with non-array spreadable objects
        var arrs = args.slice(-1);
        var size = 0;

        for (int i = 0; i < arrs.n(); i++) {
            if (arrs.get(i) instanceof ArrayValue) size += arrs.convert(i, ArrayValue.class).size();
            else i++;
        }

        var res = new ArrayValue(size);

        for (int i = 0, j = 0; i < arrs.n(); i++) {
            if (arrs.get(i) instanceof ArrayValue) {
                var arrEl = arrs.convert(i, ArrayValue.class);
                int n = arrEl.size();
                arrEl.copyTo(args.ctx, res, 0, j, n);
                j += n;
            }
            else {
                res.set(args.ctx, j++, arrs.get(i));
            }
        }

        return res;
    }
    @Expose public static ArrayValue __sort(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var cmp = args.convert(0, FunctionValue.class);

        var defaultCmp = new NativeFunction("", _args -> {
            return _args.getString(0).compareTo(_args.getString(1));
        });

        arr.sort((a, b) -> {
            var res = Values.toNumber(args.ctx, (cmp == null ? defaultCmp : cmp).call(args.ctx, null, a, b));
            if (res < 0) return -1;
            if (res > 0) return 1;
            return 0;
        });
        return arr;
    }

    @Expose public static ArrayValue __fill(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var val = args.get(0);
        var start = normalizeI(arr.size(), args.getInt(1, 0), true);
        var end = normalizeI(arr.size(), args.getInt(2, arr.size()), true);

        for (; start < end; start++) arr.set(args.ctx, start, val);

        return arr;
    }
    @Expose public static boolean __every(Arguments args) {
        var arr = args.self(ArrayValue.class);

        for (var i = 0; i < arr.size(); i++) {
            if (arr.has(i) && !Values.toBoolean(Values.call(
                args.ctx, args.get(0), args.get(1),
                arr.get(i), i, arr
            ))) return false;
        }

        return true;
    }
    @Expose public static boolean __some(Arguments args) {
        var arr = args.self(ArrayValue.class);

        for (var i = 0; i < arr.size(); i++) {
            if (arr.has(i) && Values.toBoolean(Values.call(
                args.ctx, args.get(0), args.get(1),
                arr.get(i), i, arr
            ))) return true;
        }

        return false;
    }
    @Expose public static ArrayValue __filter(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var res = new ArrayValue(arr.size());

        for (int i = 0, j = 0; i < arr.size(); i++) {
            if (arr.has(i) && Values.toBoolean(Values.call(
                args.ctx, args.get(0), args.get(1),
                arr.get(i), i, arr
            ))) res.set(args.ctx, j++, arr.get(i));
        }

        return res;
    }
    @Expose public static ArrayValue __map(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var res = new ArrayValue(arr.size());
        res.setSize(arr.size());

        for (int i = 0; i < arr.size(); i++) {
            if (arr.has(i)) res.set(args.ctx, i, Values.call(args.ctx, args.get(0), args.get(1), arr.get(i), i, arr));
        }
        return res;
    }
    @Expose public static void __forEach(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var func = args.convert(0, FunctionValue.class);
        var thisArg = args.get(1);

        for (int i = 0; i < arr.size(); i++) {
            if (arr.has(i)) func.call(args.ctx, thisArg, arr.get(i), i, arr);
        }
    }

    @Expose public static Object __reduce(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var func = args.convert(0, FunctionValue.class);
        var res = args.get(1);
        var i = 0;

        if (args.n() < 2) {
            for (; i < arr.size(); i++) {
                if (arr.has(i)){
                    res = arr.get(i++);
                    break;
                }
            }
        }

        for (; i < arr.size(); i++) {
            if (arr.has(i)) {
                res = func.call(args.ctx, null, res, arr.get(i), i, arr);
            }
        }

        return res;
    }
    @Expose public static Object __reduceRight(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var func = args.convert(0, FunctionValue.class);
        var res = args.get(1);
        var i = arr.size();

        if (args.n() < 2) {
            while (!arr.has(i--) && i >= 0) {
                res = arr.get(i);
            }
        }
        else i--;

        for (; i >= 0; i--) {
            if (arr.has(i)) {
                res = func.call(args.ctx, null, res, arr.get(i), i, arr);
            }
        }

        return res;
    }

    @Expose public static ArrayValue __flat(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var depth = args.getInt(0, 1);
        var res = new ArrayValue(arr.size());
        var stack = new Stack<Object>();
        var depths = new Stack<Integer>();

        stack.push(arr);
        depths.push(-1);

        while (!stack.empty()) {
            var el = stack.pop();
            int d = depths.pop();

            if ((d == -1 || d < depth) && el instanceof ArrayValue) {
                var arrEl = (ArrayValue)el;
                for (int i = arrEl.size() - 1; i >= 0; i--) {
                    if (!arrEl.has(i)) continue;
                    stack.push(arrEl.get(i));
                    depths.push(d + 1);
                }
            }
            else res.set(args.ctx, res.size(), el);
        }

        return res;
    }
    @Expose public static ArrayValue __flatMap(Arguments args) {
        return __flat(new Arguments(args.ctx, __map(args), 1));
    }

    @Expose public static Object __find(Arguments args) {
        var arr = args.self(ArrayValue.class);

        for (int i = 0; i < arr.size(); i++) {
            if (arr.has(i) && Values.toBoolean(Values.call(
                args.ctx, args.get(0), args.get(1),
                arr.get(i), i, args.self
            ))) return arr.get(i);
        }

        return null;
    }
    @Expose public static Object __findLast(Arguments args) {
        var arr = args.self(ArrayValue.class);

        for (var i = arr.size() - 1; i >= 0; i--) {
            if (arr.has(i) && Values.toBoolean(Values.call(
                args.ctx, args.get(0), args.get(1),
                arr.get(i), i, args.self
            ))) return arr.get(i);
        }

        return null;
    }

    @Expose public static int __findIndex(Arguments args) {
        var arr = args.self(ArrayValue.class);

        for (int i = 0; i < arr.size(); i++) {
            if (arr.has(i) && Values.toBoolean(Values.call(
                args.ctx, args.get(0), args.get(1),
                arr.get(i), i, args.self
            ))) return i;
        }

        return -1;
    }
    @Expose public static int __findLastIndex(Arguments args) {
        var arr = args.self(ArrayValue.class);

        for (var i = arr.size() - 1; i >= 0; i--) {
            if (arr.has(i) && Values.toBoolean(Values.call(
                args.ctx, args.get(0), args.get(1),
                arr.get(i), i, args.self
            ))) return i;
        }

        return -1;
    }

    @Expose public static int __indexOf(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var val = args.get(0);
        var start = normalizeI(arr.size(), args.getInt(1), true);

        for (int i = start; i < arr.size(); i++) {
            if (Values.strictEquals(args.ctx, arr.get(i), val)) return i;
        }

        return -1;
    }
    @Expose public static int __lastIndexOf(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var val = args.get(0);
        var start = normalizeI(arr.size(), args.getInt(1), true);

        for (int i = arr.size(); i >= start; i--) {
            if (Values.strictEquals(args.ctx, arr.get(i), val)) return i;
        }

        return -1;
    }

    @Expose public static boolean __includes(Arguments args) {
        return __indexOf(args) >= 0;
    }

    @Expose public static Object __pop(Arguments args) {
        var arr = args.self(ArrayValue.class);
        if (arr.size() == 0) return null;

        var val = arr.get(arr.size() - 1);
        arr.shrink(1);
        return val;
    }
    @Expose public static int __push(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var values = args.args;

        arr.copyFrom(args.ctx, values, 0, arr.size(), values.length);
        return arr.size();
    }

    @Expose public static Object __shift(Arguments args) {
        var arr = args.self(ArrayValue.class);

        if (arr.size() == 0) return null;
        var val = arr.get(0);

        arr.move(1, 0, arr.size());
        arr.shrink(1);
        return val;
    }
    @Expose public static int __unshift(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var values = args.slice(0).args;

        arr.move(0, values.length, arr.size());
        arr.copyFrom(args.ctx, values, 0, 0, values.length);
        return arr.size();
    }

    @Expose public static ArrayValue __slice(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var start = normalizeI(arr.size(), args.getInt(0), true);
        var end = normalizeI(arr.size(), args.getInt(1, arr.size()), true);

        var res = new ArrayValue(end - start);
        arr.copyTo(args.ctx, res, start, 0, end - start);
        return res;
    }

    @Expose public static ArrayValue __splice(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var start = normalizeI(arr.size(), args.getInt(0), true);
        var deleteCount = normalizeI(arr.size(), args.getInt(1, arr.size()), true);
        var items = args.slice(2).args;

        if (start + deleteCount >= arr.size()) deleteCount = arr.size() - start;

        var size = arr.size() - deleteCount + items.length;
        var res = new ArrayValue(deleteCount);
        arr.copyTo(args.ctx, res, start, 0, deleteCount);
        arr.move(start + deleteCount, start + items.length, arr.size() - start - deleteCount);
        arr.copyFrom(args.ctx, items, 0, start, items.length);
        arr.setSize(size);

        return res;
    }
    @Expose public static String __toString(Arguments args) {
        return __join(new Arguments(args.ctx, args.self, ","));
    }

    @Expose public static String __join(Arguments args) {
        var arr = args.self(ArrayValue.class);
        var sep = args.getString(0, ", ");
        var res = new StringBuilder();
        var comma = false;

        for (int i = 0; i < arr.size(); i++) {
            if (!arr.has(i)) continue;

            if (comma) res.append(sep);
            comma = true;

            var el = arr.get(i);
            if (el == null || el == Values.NULL) continue;

            res.append(Values.toString(args.ctx, el));
        }

        return res.toString();
    }

    @Expose(target = ExposeTarget.STATIC)
    public static boolean __isArray(Arguments args) {
        return args.get(0) instanceof ArrayValue;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static ArrayValue __of(Arguments args) {
        return new ArrayValue(args.ctx, args.slice(0).args);
    }

    @ExposeConstructor public static ArrayValue __constructor(Arguments args) {
        ArrayValue res;

        if (args.n() == 1 && args.get(0) instanceof Number) {
            var len = args.getInt(0);
            res = new ArrayValue(len);
            res.setSize(len);
        }
        else {
            var val = args.args;
            res = new ArrayValue(val.length);
            res.copyFrom(args.ctx, val, 0, 0, val.length);
        }

        return res;
    }
}
