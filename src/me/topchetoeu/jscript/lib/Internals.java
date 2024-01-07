package me.topchetoeu.jscript.lib;

import java.io.IOException;
import java.util.HashMap;

import me.topchetoeu.jscript.Reading;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.scope.GlobalScope;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.Expose;
import me.topchetoeu.jscript.interop.ExposeField;
import me.topchetoeu.jscript.interop.ExposeTarget;
import me.topchetoeu.jscript.modules.ModuleRepo;

public class Internals {
    private static final Symbol THREADS = new Symbol("Internals.threads");
    private static final Symbol I = new Symbol("Internals.i");

    @Expose(target = ExposeTarget.STATIC)
    public static Object __require(Arguments args) {
        var repo = ModuleRepo.get(args.ctx);

        if (repo != null) {
            var res = repo.getModule(args.ctx, ModuleRepo.cwd(args.ctx), args.getString(0));
            res.load(args.ctx);
            return res.value();
        }

        else throw EngineException.ofError("Modules are not supported.");
    }

    @Expose(target = ExposeTarget.STATIC)
    public static Object __log(Arguments args) {
        for (var arg : args.args) {
            Values.printValue(args.ctx, arg);
            System.out.print(" ");
        }
        System.out.println();

        return args.get(0);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static String __readline() {
        try {
            return Reading.readline();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Expose(target = ExposeTarget.STATIC)
    public static Thread __setTimeout(Arguments args) {
        var func = args.convert(0, FunctionValue.class);
        var delay = args.getDouble(1);
        var arguments = args.slice(2).args;

        var thread = new Thread(() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            try { Thread.sleep(ms, ns); }
            catch (InterruptedException e) { return; }

            args.ctx.engine.pushMsg(false, args.ctx.environment, func, null, arguments);
        });

        thread.start();
        var i = args.ctx.init(I, 1);
        args.ctx.add(I, i + 1);
        args.ctx.init(THREADS, new HashMap<Integer, Thread>()).put(i, thread);

        return thread;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static Thread __setInterval(Arguments args) {
        var func = args.convert(0, FunctionValue.class);
        var delay = args.getDouble(1);
        var arguments = args.slice(2).args;

        var thread = new Thread(() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            while (true) {
                try {
                    Thread.sleep(ms, ns);
                }
                catch (InterruptedException e) { return; }

                args.ctx.engine.pushMsg(false, args.ctx.environment, func, null, arguments);
            }
        });
        thread.start();
        var i = args.ctx.init(I, 1);
        args.ctx.add(I, i + 1);
        args.ctx.init(THREADS, new HashMap<Integer, Thread>()).put(i, thread);

        return thread;
    }

    @Expose(target = ExposeTarget.STATIC)
    public static void __clearTimeout(Arguments args) {
        var i = args.getInt(0);
        HashMap<Integer, Thread> map = args.ctx.get(THREADS);
        if (map == null) return;

        var thread = map.get(i);
        if (thread == null) return;

        thread.interrupt();
        map.remove(i);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static void __clearInterval(Arguments args) {
        __clearTimeout(args);
    }

    @Expose(target = ExposeTarget.STATIC)
    public static double __parseInt(Arguments args) {
        return NumberLib.__parseInt(args);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __parseFloat(Arguments args) {
        return NumberLib.__parseFloat(args);
    }

    @Expose(target = ExposeTarget.STATIC)
    public static boolean __isNaN(Arguments args) {
        return NumberLib.__isNaN(args);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static boolean __isFinite(Arguments args) {
        return NumberLib.__isFinite(args);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static boolean __isInfinite(Arguments args) {
        return NumberLib.__isInfinite(args);
    }

    @ExposeField(target = ExposeTarget.STATIC)
    public static double __NaN = Double.NaN;
    @ExposeField(target = ExposeTarget.STATIC)
    public static double __Infinity = Double.POSITIVE_INFINITY;

    @Expose(target = ExposeTarget.STATIC)
    public static String __encodeURIComponent(Arguments args) {
        return EncodingLib.__encodeURIComponent(args);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static String __decodeURIComponent(Arguments args) {
        return EncodingLib.__decodeURIComponent(args);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static String __encodeURI(Arguments args) {
        return EncodingLib.__encodeURI(args);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static String __decodeURI(Arguments args) {
        return EncodingLib.__decodeURI(args);
    }

    public static Environment apply(Environment env) {
        var wp = env.wrappers;
        var glob = env.global = new GlobalScope(wp.getNamespace(Internals.class));

        glob.define(null, "Math", false, wp.getNamespace(MathLib.class));
        glob.define(null, "JSON", false, wp.getNamespace(JSONLib.class));
        glob.define(null, "Encoding", false, wp.getNamespace(EncodingLib.class));
        glob.define(null, "Filesystem", false, wp.getNamespace(FilesystemLib.class));

        glob.define(false, wp.getConstr(DateLib.class));
        glob.define(false, wp.getConstr(ObjectLib.class));
        glob.define(false, wp.getConstr(FunctionLib.class));
        glob.define(false, wp.getConstr(ArrayLib.class));

        glob.define(false, wp.getConstr(BooleanLib.class));
        glob.define(false, wp.getConstr(NumberLib.class));
        glob.define(false, wp.getConstr(StringLib.class));
        glob.define(false, wp.getConstr(SymbolLib.class));

        glob.define(false, wp.getConstr(PromiseLib.class));
        glob.define(false, wp.getConstr(RegExpLib.class));
        glob.define(false, wp.getConstr(MapLib.class));
        glob.define(false, wp.getConstr(SetLib.class));

        glob.define(false, wp.getConstr(ErrorLib.class));
        glob.define(false, wp.getConstr(SyntaxErrorLib.class));
        glob.define(false, wp.getConstr(TypeErrorLib.class));
        glob.define(false, wp.getConstr(RangeErrorLib.class));

        env.add(Environment.OBJECT_PROTO, wp.getProto(ObjectLib.class));
        env.add(Environment.FUNCTION_PROTO, wp.getProto(FunctionLib.class));
        env.add(Environment.ARRAY_PROTO, wp.getProto(ArrayLib.class));

        env.add(Environment.BOOL_PROTO, wp.getProto(BooleanLib.class));
        env.add(Environment.NUMBER_PROTO, wp.getProto(NumberLib.class));
        env.add(Environment.STRING_PROTO, wp.getProto(StringLib.class));
        env.add(Environment.SYMBOL_PROTO, wp.getProto(SymbolLib.class));

        env.add(Environment.ERROR_PROTO, wp.getProto(ErrorLib.class));
        env.add(Environment.SYNTAX_ERR_PROTO, wp.getProto(SyntaxErrorLib.class));
        env.add(Environment.TYPE_ERR_PROTO, wp.getProto(TypeErrorLib.class));
        env.add(Environment.RANGE_ERR_PROTO, wp.getProto(RangeErrorLib.class));

        Values.setPrototype(Context.NULL, wp.getProto(ObjectLib.class), null);
        env.add(Environment.REGEX_CONSTR, wp.getConstr(RegExpLib.class));

        return env;
    }
}
