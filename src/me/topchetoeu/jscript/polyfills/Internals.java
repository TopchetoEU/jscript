package me.topchetoeu.jscript.polyfills;

import java.util.HashMap;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeGetter;

public class Internals {
    private HashMap<Integer, Thread> intervals = new HashMap<>();
    private HashMap<Integer, Thread> timeouts = new HashMap<>();
    private HashMap<String, Symbol> symbols = new HashMap<>();
    private int nextId = 0;

    @Native
    public double parseFloat(CallContext ctx, Object val) throws InterruptedException {
        return Values.toNumber(ctx, val);
    }

    @NativeGetter("symbolProto")
    public ObjectValue symbolProto(CallContext ctx) {
        return ctx.engine().symbolProto();
    }
    @Native
    public final Object apply(CallContext ctx, FunctionValue func, Object th, ArrayValue args) throws InterruptedException {
        return func.call(ctx, th, args.toArray());
    }
    @Native
    public boolean defineProp(ObjectValue obj, Object key, FunctionValue getter, FunctionValue setter, boolean enumerable, boolean configurable) {
        return obj.defineProperty(key, getter, setter, configurable, enumerable);
    }
    @Native
    public boolean defineField(ObjectValue obj, Object key, Object val, boolean writable, boolean enumerable, boolean configurable) {
        return obj.defineProperty(key, val, writable, configurable, enumerable);
    }

    @Native
    public int strlen(String str) {
        return str.length();
    }
    @Native
    public String substring(String str, int start, int end) {
        if (start > end) return substring(str, end, start);

        if (start < 0) start = 0;
        if (start >= str.length()) return "";

        if (end < 0) end = 0;
        if (end > str.length()) end = str.length();

        return str.substring(start, end);
    }
    @Native
    public String toLower(String str) {
        return str.toLowerCase();
    }
    @Native
    public String toUpper(String str) {
        return str.toUpperCase();
    }
    @Native
    public int toCharCode(String str) {
        return str.codePointAt(0);
    }
    @Native
    public String fromCharCode(int code) {
        return Character.toString((char)code);
    }
    @Native
    public boolean startsWith(String str, String term, int offset) {
        return str.startsWith(term, offset);
    }
    @Native
    public boolean endsWith(String str, String term, int offset) {
        try {
            return str.substring(0, offset).endsWith(term);
        }
        catch (IndexOutOfBoundsException e) { return false; }

    }

    @Native
    public int setInterval(CallContext ctx, FunctionValue func, double delay) {
        var thread = new Thread((Runnable)() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            while (true) {
                try {
                    Thread.sleep(ms, ns);
                }
                catch (InterruptedException e) { return; }

                ctx.engine().pushMsg(false, func, ctx.data(), null);
            }
        });
        thread.start();

        intervals.put(++nextId, thread);

        return nextId;
    }
    @Native
    public int setTimeout(CallContext ctx, FunctionValue func, double delay) {
        var thread = new Thread((Runnable)() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 1000000);

            try {
                Thread.sleep(ms, ns);
            }
            catch (InterruptedException e) { return; }

            ctx.engine().pushMsg(false, func, ctx.data(), null);
        });
        thread.start();

        timeouts.put(++nextId, thread);

        return nextId;
    }

    @Native
    public void clearInterval(int id) {
        var thread = intervals.remove(id);
        if (thread != null) thread.interrupt();
    }
    @Native
    public void clearTimeout(int id) {
        var thread = timeouts.remove(id);
        if (thread != null) thread.interrupt();
    }

    @Native
    public void sort(CallContext ctx, ArrayValue arr, FunctionValue cmp) {
        arr.sort((a, b) -> {
            try {
                var res = Values.toNumber(ctx, cmp.call(ctx, null, a, b));
                if (res < 0) return -1;
                if (res > 0) return 1;
                return 0;
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return 0;
            }
        });
    }

    @Native
    public void special(FunctionValue... funcs) {
        for (var func : funcs) {
            func.special = true;
        }
    }

    @Native
    public Symbol symbol(String name, boolean unique) {
        if (!unique && symbols.containsKey(name)) {
            return symbols.get(name);
        }
        else {
            var val = new Symbol(name);
            if (!unique) symbols.put(name, val);
            return val;
        }
    }
    @Native
    public String symStr(Symbol symbol) {
        return symbol.value;
    }

    @Native
    public void freeze(ObjectValue val) {
        val.freeze();
    }
    @Native
    public void seal(ObjectValue val) {
        val.seal();
    }
    @Native
    public void preventExtensions(ObjectValue val) {
        val.preventExtensions();
    }

    @Native
    public boolean extensible(Object val) {
        return Values.isObject(val) && Values.object(val).extensible();
    }

    @Native
    public ArrayValue keys(CallContext ctx, Object obj, boolean onlyString) throws InterruptedException {
        var res = new ArrayValue();

        var i = 0;
        var list = Values.getMembers(ctx, obj, true, false);

        for (var el : list) {
            if (el instanceof Symbol && onlyString) continue;
            res.set(i++, el);
        }

        return res;
    }
    @Native
    public ArrayValue ownPropKeys(CallContext ctx, Object obj, boolean symbols) throws InterruptedException {
        var res = new ArrayValue();

        if (Values.isObject(obj)) {
            var i = 0;
            var list = Values.object(obj).keys(true);

            for (var el : list) {
                if (el instanceof Symbol == symbols) res.set(i++, el);
            }
        }

        return res;
    }
    @Native
    public ObjectValue ownProp(CallContext ctx, ObjectValue val, Object key) throws InterruptedException {
        return val.getMemberDescriptor(ctx, key);
    }

    @Native
    public Object require(CallContext ctx, Object name) throws InterruptedException {
        var res = ctx.engine().modules().tryLoad(ctx, Values.toString(ctx, name));
        if (res == null) throw EngineException.ofError("The module '" + name + "' doesn\'t exist.");
        return res.exports();
    }

    @NativeGetter("err")
    public ObjectValue errProto(CallContext ctx) {
        return ctx.engine.errorProto();
    }
    @NativeGetter("syntax")
    public ObjectValue syntaxProto(CallContext ctx) {
        return ctx.engine.syntaxErrorProto();
    }
    @NativeGetter("range")
    public ObjectValue rangeProto(CallContext ctx) {
        return ctx.engine.rangeErrorProto();
    }
    @NativeGetter("type")
    public ObjectValue typeProto(CallContext ctx) {
        return ctx.engine.typeErrorProto();
    }
}
