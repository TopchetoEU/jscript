package me.topchetoeu.jscript.polyfills;

import java.util.HashMap;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.DataKey;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.scope.GlobalScope;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeWrapperProvider;

public class Internals {
    private static final DataKey<HashMap<Integer, Thread>> THREADS = new DataKey<>();
    private static final DataKey<Integer> I = new DataKey<>();

    @Native public static FunctionValue bind(FunctionValue func, Object thisArg) throws InterruptedException {
        return FunctionPolyfill.bind(func, thisArg);
    }
    @Native public static void log(Context ctx, Object ...args) throws InterruptedException {
        for (var arg : args) {
            Values.printValue(ctx, arg);
        }
        System.out.println();
    }

    @Native public static int setTimeout(Context ctx, FunctionValue func, int delay, Object ...args) {
        var thread = new Thread(() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            try {
                Thread.sleep(ms, ns);
            }
            catch (InterruptedException e) { return; }

            ctx.message.engine.pushMsg(false, ctx.message, func, null, args);
        });
        thread.start();

        int i = ctx.env.data.increase(I, 1, 0);
        var threads = ctx.env.data.add(THREADS, new HashMap<>());
        threads.put(++i, thread);
        return i;
    }
    @Native public static int setInterval(Context ctx, FunctionValue func, int delay, Object ...args) {
        var thread = new Thread(() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            while (true) {
                try {
                    Thread.sleep(ms, ns);
                }
                catch (InterruptedException e) { return; }
    
                ctx.message.engine.pushMsg(false, ctx.message, func, null, args);
            }
        });
        thread.start();

        int i = ctx.env.data.increase(I, 1, 0);
        var threads = ctx.env.data.add(THREADS, new HashMap<>());
        threads.put(++i, thread);
        return i;
    }

    @Native public static void clearTimeout(Context ctx, int i) {
        var threads = ctx.env.data.add(THREADS, new HashMap<>());

        var thread = threads.remove(i);
        if (thread != null) thread.interrupt();
    }
    @Native public static void clearInterval(Context ctx, int i) {
        clearTimeout(ctx, i);
    }

    @Native public static double parseInt(Context ctx, String val) throws InterruptedException {
        return NumberPolyfill.parseInt(ctx, val);
    }
    @Native public static double parseFloat(Context ctx, String val) throws InterruptedException {
        return NumberPolyfill.parseFloat(ctx, val);
    }

    public void apply(Environment env) {
        var wp = env.wrappersProvider;
        var glob = env.global = new GlobalScope(NativeWrapperProvider.makeNamespace(env, Internals.class));

        glob.define(null, "Object", false, wp.getConstr(ObjectPolyfill.class));
        glob.define(null, "Function", false, wp.getConstr(FunctionPolyfill.class));
        glob.define(null, "Array", false, wp.getConstr(ArrayPolyfill.class));

        glob.define(null, "Boolean", false, wp.getConstr(BooleanPolyfill.class));
        glob.define(null, "Number", false, wp.getConstr(NumberPolyfill.class));
        glob.define(null, "String", false, wp.getConstr(StringPolyfill.class));
        glob.define(null, "Symbol", false, wp.getConstr(SymbolPolyfill.class));

        glob.define(null, "Promise", false, wp.getConstr(PromisePolyfill.class));
        glob.define(null, "RegExp", false, wp.getConstr(RegExpPolyfill.class));
        glob.define(null, "Map", false, wp.getConstr(MapPolyfill.class));
        glob.define(null, "Set", false, wp.getConstr(SetPolyfill.class));

        glob.define(null, "Error", false, wp.getConstr(ErrorPolyfill.class));
        glob.define(null, "SyntaxError", false, wp.getConstr(SyntaxErrorPolyfill.class));
        glob.define(null, "TypeError", false, wp.getConstr(TypeErrorPolyfill.class));
        glob.define(null, "RangeError", false, wp.getConstr(RangeErrorPolyfill.class));

        env.setProto("object", wp.getProto(ObjectPolyfill.class));
        env.setProto("function", wp.getProto(FunctionPolyfill.class));
        env.setProto("array", wp.getProto(ArrayPolyfill.class));

        env.setProto("bool", wp.getProto(BooleanPolyfill.class));
        env.setProto("number", wp.getProto(NumberPolyfill.class));
        env.setProto("string", wp.getProto(StringPolyfill.class));
        env.setProto("symbol", wp.getProto(SymbolPolyfill.class));

        env.setProto("error", wp.getProto(ErrorPolyfill.class));
        env.setProto("syntaxErr", wp.getProto(SyntaxErrorPolyfill.class));
        env.setProto("typeErr", wp.getProto(TypeErrorPolyfill.class));
        env.setProto("rangeErr", wp.getProto(RangeErrorPolyfill.class));

        wp.getProto(ObjectPolyfill.class).setPrototype(null, null);

        System.out.println("Loaded polyfills!");
    }
}
