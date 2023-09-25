package me.topchetoeu.jscript.polyfills;

import me.topchetoeu.jscript.engine.Context;
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
    public final Environment targetEnv;

    @Native public final FunctionValue object, function, promise, array, bool, number, string;

    @Native public void markSpecial(FunctionValue ...funcs) {
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
    @Native public Object apply(Context ctx, FunctionValue func, Object thisArg, ArrayValue args, Environment env) throws InterruptedException {
        if (env != null) ctx = new Context(env, ctx.message);
        return func.call(ctx, thisArg, args.toArray());
    }
    @Native public FunctionValue delay(Context ctx, double delay, FunctionValue callback) throws InterruptedException {
        var thread = new Thread((Runnable)() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            try {
                Thread.sleep(ms, ns);
            }
            catch (InterruptedException e) { return; }

            ctx.message.engine.pushMsg(false, ctx.message, callback, null);
        });
        thread.start();

        return new NativeFunction((_ctx, thisArg, args) -> {
            thread.interrupt();
            return null;
        });
    }
    @Native public void pushMessage(Context ctx, boolean micro, FunctionValue func, Object thisArg, Object[] args) {
        ctx.message.engine.pushMsg(micro, ctx.message, func, thisArg, args);
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

    @Native public void log(Context ctx, Object ...args) throws InterruptedException {
        for (var arg : args) {
            Values.printValue(ctx, arg);
        }
        System.out.println();
    }

    @Native public boolean isArray(Object obj) {
        return obj instanceof ArrayValue;
    }
    @Native public GeneratorPolyfill generator(FunctionValue obj) {
        return new GeneratorPolyfill(obj);
    }

    @Native public boolean defineField(Context ctx, ObjectValue obj, Object key, Object val, boolean writable, boolean enumerable, boolean configurable) {
        return obj.defineProperty(ctx, key, val, writable, configurable, enumerable);
    }
    @Native public boolean defineProp(Context ctx, ObjectValue obj, Object key, FunctionValue getter, FunctionValue setter, boolean enumerable, boolean configurable) {
        return obj.defineProperty(ctx, key, getter, setter, configurable, enumerable);
    }

    @Native public ArrayValue keys(Context ctx, Object obj, boolean onlyString) throws InterruptedException {
        var res = new ArrayValue();

        var i = 0;
        var list = Values.getMembers(ctx, obj, true, false);

        for (var el : list) res.set(ctx, i++, el);

        return res;
    }
    @Native public ArrayValue ownPropKeys(Context ctx, Object obj, boolean symbols) throws InterruptedException {
        var res = new ArrayValue();

        if (Values.isObject(obj)) {
            var i = 0;
            var list = Values.object(obj).keys(true);

            for (var el : list) res.set(ctx, i++, el);
        }

        return res;
    }
    @Native public ObjectValue ownProp(Context ctx, ObjectValue val, Object key) throws InterruptedException {
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

    @Native public void sort(Context ctx, ArrayValue arr, FunctionValue cmp) {
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

    public Internals(Environment targetEnv) {
        this.targetEnv = targetEnv;
        this.object = targetEnv.wrappersProvider.getConstr(ObjectPolyfill.class);
        this.function = targetEnv.wrappersProvider.getConstr(FunctionPolyfill.class);
        this.promise = targetEnv.wrappersProvider.getConstr(PromisePolyfill.class);
        this.array = targetEnv.wrappersProvider.getConstr(ArrayPolyfill.class);
        this.bool = targetEnv.wrappersProvider.getConstr(BooleanPolyfill.class);
        this.number = targetEnv.wrappersProvider.getConstr(NumberPolyfill.class);
        this.string = targetEnv.wrappersProvider.getConstr(StringPolyfill.class);
    }
}
