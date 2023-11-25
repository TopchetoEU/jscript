package me.topchetoeu.jscript.lib;

import java.io.IOException;
import java.util.HashMap;

import me.topchetoeu.jscript.Reading;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.DataKey;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.scope.GlobalScope;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeGetter;

public class Internals {
    private static final DataKey<HashMap<Integer, Thread>> THREADS = new DataKey<>();
    private static final DataKey<Integer> I = new DataKey<>();


    @Native public static Object log(Context ctx, Object ...args) {
        for (var arg : args) {
            Values.printValue(ctx, arg);
            System.out.print(" ");
        }
        System.out.println();

        if (args.length == 0) return null;
        else return args[0];
    }
    @Native public static String readline(Context ctx) {
        try {
            return Reading.read();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Native public static int setTimeout(Context ctx, FunctionValue func, int delay, Object ...args) {
        var thread = new Thread(() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            try {
                Thread.sleep(ms, ns);
            }
            catch (InterruptedException e) { return; }

            ctx.engine.pushMsg(false, ctx, func, null, args);
        });
        thread.start();

        int i = ctx.environment().data.increase(I, 1, 0);
        var threads = ctx.environment().data.get(THREADS, new HashMap<>());
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
    
                ctx.engine.pushMsg(false, ctx, func, null, args);
            }
        });
        thread.start();

        int i = ctx.environment().data.increase(I, 1, 0);
        var threads = ctx.environment().data.get(THREADS, new HashMap<>());
        threads.put(++i, thread);
        return i;
    }

    @Native public static void clearTimeout(Context ctx, int i) {
        var threads = ctx.environment().data.get(THREADS, new HashMap<>());

        var thread = threads.remove(i);
        if (thread != null) thread.interrupt();
    }
    @Native public static void clearInterval(Context ctx, int i) {
        clearTimeout(ctx, i);
    }

    @Native public static double parseInt(Context ctx, String val) {
        return NumberLib.parseInt(ctx, val);
    }
    @Native public static double parseFloat(Context ctx, String val) {
        return NumberLib.parseFloat(ctx, val);
    }

    @Native public static boolean isNaN(Context ctx, double val) {
        return NumberLib.isNaN(ctx, val);
    }
    @Native public static boolean isFinite(Context ctx, double val) {
        return NumberLib.isFinite(ctx, val);
    }
    @Native public static boolean isInfinite(Context ctx, double val) {
        return NumberLib.isInfinite(ctx, val);
    }

    @NativeGetter public static double NaN(Context ctx) {
        return Double.NaN;
    }
    @NativeGetter public static double Infinity(Context ctx) {
        return Double.POSITIVE_INFINITY;
    }

    public static Environment apply(Environment env) {
        var wp = env.wrappers;
        var glob = env.global = new GlobalScope(wp.getNamespace(Internals.class));

        glob.define(null, "Math", false, wp.getNamespace(MathLib.class));
        glob.define(null, "JSON", false, wp.getNamespace(JSONLib.class));
        glob.define(null, "Encoding", false, wp.getNamespace(EncodingLib.class));
        glob.define(null, "Filesystem", false, wp.getNamespace(FilesystemLib.class));

        glob.define(null, "Date", false, wp.getConstr(DateLib.class));
        glob.define(null, "Object", false, wp.getConstr(ObjectLib.class));
        glob.define(null, "Function", false, wp.getConstr(FunctionLib.class));
        glob.define(null, "Array", false, wp.getConstr(ArrayLib.class));

        glob.define(null, "Boolean", false, wp.getConstr(BooleanLib.class));
        glob.define(null, "Number", false, wp.getConstr(NumberLib.class));
        glob.define(null, "String", false, wp.getConstr(StringLib.class));
        glob.define(null, "Symbol", false, wp.getConstr(SymbolLib.class));

        glob.define(null, "Promise", false, wp.getConstr(PromiseLib.class));
        glob.define(null, "RegExp", false, wp.getConstr(RegExpLib.class));
        glob.define(null, "Map", false, wp.getConstr(MapLib.class));
        glob.define(null, "Set", false, wp.getConstr(SetLib.class));

        glob.define(null, "Error", false, wp.getConstr(ErrorLib.class));
        glob.define(null, "SyntaxError", false, wp.getConstr(SyntaxErrorLib.class));
        glob.define(null, "TypeError", false, wp.getConstr(TypeErrorLib.class));
        glob.define(null, "RangeError", false, wp.getConstr(RangeErrorLib.class));

        env.setProto("object", wp.getProto(ObjectLib.class));
        env.setProto("function", wp.getProto(FunctionLib.class));
        env.setProto("array", wp.getProto(ArrayLib.class));

        env.setProto("bool", wp.getProto(BooleanLib.class));
        env.setProto("number", wp.getProto(NumberLib.class));
        env.setProto("string", wp.getProto(StringLib.class));
        env.setProto("symbol", wp.getProto(SymbolLib.class));

        env.setProto("error", wp.getProto(ErrorLib.class));
        env.setProto("syntaxErr", wp.getProto(SyntaxErrorLib.class));
        env.setProto("typeErr", wp.getProto(TypeErrorLib.class));
        env.setProto("rangeErr", wp.getProto(RangeErrorLib.class));

        wp.getProto(ObjectLib.class).setPrototype(null, null);
        env.regexConstructor = wp.getConstr(RegExpLib.class);

        return env;
    }
}
