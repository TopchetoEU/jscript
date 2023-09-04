package me.topchetoeu.jscript.polyfills;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;

public class Internals {
    @Native public void markSpecial(FunctionValue... funcs) {
        for (var func : funcs) {
            func.special = true;
        }
    }
    @Native public Environment getEnv(Object func) {
        if (func instanceof CodeFunction) return ((CodeFunction)func).environment;
        else return null;
    }
    @Native public Object setEnv(Object func, Environment env) {
        if (func instanceof CodeFunction) ((CodeFunction)func).environment = env;
        return func;
    }
    @Native public Object apply(CallContext ctx, FunctionValue func, Object thisArg, ArrayValue args) throws InterruptedException {
        return func.call(ctx, thisArg, args.toArray());
    }
    @Native public FunctionValue delay(CallContext ctx, double delay, FunctionValue callback) throws InterruptedException {
        var thread = new Thread((Runnable)() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            try {
                Thread.sleep(ms, ns);
            }
            catch (InterruptedException e) { return; }

            ctx.engine.pushMsg(false, ctx.data(), ctx.environment, callback, null);
        });
        thread.start();

        return new NativeFunction((_ctx, thisArg, args) -> {
            thread.interrupt();
            return null;
        });
    }
    @Native public void pushMessage(CallContext ctx, boolean micro, FunctionValue func, Object thisArg, Object[] args) {
        ctx.engine.pushMsg(micro, ctx.data(), ctx.environment, func, thisArg, args);
    }

    @Native public int strlen(String str) {
        return str.length();
    }
    @Native("char") public int _char(String str) {
        return str.charAt(0);
    }
    @Native public String stringFromChars(char[] str) {
        return new String(str);
    }
    @Native public String stringFromStrings(String[] str) {
        var res = new char[str.length];

        for (var i = 0; i < str.length; i++) res[i] = str[i].charAt(0);

        return stringFromChars(res);
    }
    @Native public Symbol symbol(String str) {
        return new Symbol(str);
    }
    @Native public String symbolToString(Symbol str) {
        return str.value;
    }

    @Native public boolean isArray(Object obj) {
        return obj instanceof ArrayValue;
    }
    @Native public GeneratorFunction generator(FunctionValue obj) {
        return new GeneratorFunction(obj);
    }

    @Native public boolean defineField(CallContext ctx, ObjectValue obj, Object key, Object val, boolean writable, boolean enumerable, boolean configurable) {
        return obj.defineProperty(ctx, key, val, writable, configurable, enumerable);
    }
    @Native public boolean defineProp(CallContext ctx, ObjectValue obj, Object key, FunctionValue getter, FunctionValue setter, boolean enumerable, boolean configurable) {
        return obj.defineProperty(ctx, key, getter, setter, configurable, enumerable);
    }

    @Native public ArrayValue keys(CallContext ctx, Object obj, boolean onlyString) throws InterruptedException {
        var res = new ArrayValue();

        var i = 0;
        var list = Values.getMembers(ctx, obj, true, false);

        for (var el : list) res.set(ctx, i++, el);

        return res;
    }
    @Native public ArrayValue ownPropKeys(CallContext ctx, Object obj, boolean symbols) throws InterruptedException {
        var res = new ArrayValue();

        if (Values.isObject(obj)) {
            var i = 0;
            var list = Values.object(obj).keys(true);

            for (var el : list) res.set(ctx, i++, el);
        }

        return res;
    }
    @Native public ObjectValue ownProp(CallContext ctx, ObjectValue val, Object key) throws InterruptedException {
        return val.getMemberDescriptor(ctx, key);
    }
    @Native public void lock(ObjectValue val, String type) {
        switch (type) {
            case "ext": val.preventExtensions(); break;
            case "seal": val.seal(); break;
            case "freeze": val.freeze(); break;
        }
    }
    @Native public boolean extensible(ObjectValue val) {
        return val.extensible();
    }

    @Native public void sort(CallContext ctx, ArrayValue arr, FunctionValue cmp) {
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
}
