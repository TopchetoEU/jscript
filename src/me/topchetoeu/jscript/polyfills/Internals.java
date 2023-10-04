package me.topchetoeu.jscript.polyfills;

import java.util.HashMap;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;

public class Internals {
    private HashMap<Integer, Thread> threads = new HashMap<>();
    private int i = 0;

    @Native public FunctionValue bind(FunctionValue func, Object thisArg) throws InterruptedException {
        return FunctionPolyfill.bind(func, thisArg);
    }
    @Native public void log(Context ctx, Object ...args) throws InterruptedException {
        for (var arg : args) {
            Values.printValue(ctx, arg);
        }
        System.out.println();
    }

    @Native public int setTimeout(Context ctx, FunctionValue func, int delay, Object ...args) {
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

        threads.put(++i, thread);

        return i;
    }
    @Native public int setInterval(Context ctx, FunctionValue func, int delay, Object ...args) {
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

        threads.put(++i, thread);

        return i;
    }

    @Native public void clearTimeout(Context ctx, int i) {
        var thread = threads.remove(i);
        if (thread != null) thread.interrupt();
    }
    @Native public void clearInterval(Context ctx, int i) {
        clearTimeout(ctx, i);
    }

    public void apply(Environment env) {
        var wp = env.wrappersProvider;
        var glob = env.global;

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

        System.out.println("Loaded polyfills!");
    }
}
