package me.topchetoeu.jscript.lib;

import java.util.HashMap;

import me.topchetoeu.jscript.runtime.EventLoop;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.environment.Key;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.scope.GlobalScope;
import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.Values;
import me.topchetoeu.jscript.utils.filesystem.Filesystem;
import me.topchetoeu.jscript.utils.filesystem.Mode;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeField;
import me.topchetoeu.jscript.utils.interop.ExposeTarget;
import me.topchetoeu.jscript.utils.interop.ExposeType;
import me.topchetoeu.jscript.utils.interop.NativeWrapperProvider;
import me.topchetoeu.jscript.utils.modules.ModuleRepo;

public class Internals {
    private static final Key<HashMap<Integer, Thread>> THREADS = new Key<>();
    private static final Key<Integer> I = new Key<>();

    @Expose(target = ExposeTarget.STATIC)
    public static Object __require(Arguments args) {
        var repo = ModuleRepo.get(args.env);

        if (repo != null) {
            var res = repo.getModule(args.env, ModuleRepo.cwd(args.env), args.getString(0));
            res.load(args.env);
            return res.value();
        }

        else throw EngineException.ofError("Modules are not supported.");
    }

    @Expose(target = ExposeTarget.STATIC)
    public static Thread __setTimeout(Arguments args) {
        var func = args.convert(0, FunctionValue.class);
        var delay = args.getDouble(1);
        var arguments = args.slice(2).args;

        if (!args.env.hasNotNull(EventLoop.KEY)) throw EngineException.ofError("No event loop");

        var thread = new Thread(() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            try { Thread.sleep(ms, ns); }
            catch (InterruptedException e) { return; }

            args.env.get(EventLoop.KEY).pushMsg(() -> func.call(args.env, null, arguments), false);
        });

        thread.start();

        args.env.init(I, 1);
        args.env.init(THREADS, new HashMap<>());
        var i = args.env.get(I);

        args.env.add(I, i + 1);
        args.env.get(THREADS).put(i, thread);

        return thread;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static Thread __setInterval(Arguments args) {
        var func = args.convert(0, FunctionValue.class);
        var delay = args.getDouble(1);
        var arguments = args.slice(2).args;

        if (!args.env.hasNotNull(EventLoop.KEY)) throw EngineException.ofError("No event loop");

        var thread = new Thread(() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            while (true) {
                try {
                    Thread.sleep(ms, ns);
                }
                catch (InterruptedException e) { return; }

                args.env.get(EventLoop.KEY).pushMsg(() -> func.call(args.env, null, arguments), false);
            }
        });

        thread.start();

        args.env.init(I, 1);
        args.env.init(THREADS, new HashMap<>());
        var i = args.env.get(I);

        args.env.add(I, i + 1);
        args.env.get(THREADS).put(i, thread);

        return thread;
    }

    @Expose(target = ExposeTarget.STATIC)
    public static void __clearTimeout(Arguments args) {
        var i = args.getInt(0);
        HashMap<Integer, Thread> map = args.env.get(THREADS);
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

    @Expose(target = ExposeTarget.STATIC, type = ExposeType.GETTER)
    public static FileLib __stdin(Arguments args) {
        return new FileLib(Filesystem.get(args.env).open("std://in", Mode.READ));
    }
    @Expose(target = ExposeTarget.STATIC, type = ExposeType.GETTER)
    public static FileLib __stdout(Arguments args) {
        return new FileLib(Filesystem.get(args.env).open("std://out", Mode.READ));
    }
    @Expose(target = ExposeTarget.STATIC, type = ExposeType.GETTER)
    public static FileLib __stderr(Arguments args) {
        return new FileLib(Filesystem.get(args.env).open("std://err", Mode.READ));
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
        var wp = new NativeWrapperProvider();
        var glob = new GlobalScope(wp.getNamespace(Internals.class));

        glob.define(null, "Math", false, wp.getNamespace(MathLib.class));
        glob.define(null, "JSON", false, wp.getNamespace(JSONLib.class));
        glob.define(null, "Encoding", false, wp.getNamespace(EncodingLib.class));
        glob.define(null, "Filesystem", false, wp.getNamespace(FilesystemLib.class));

        glob.define(null, false, wp.getConstr(FileLib.class));

        glob.define(null, false, wp.getConstr(DateLib.class));
        glob.define(null, false, wp.getConstr(ObjectLib.class));
        glob.define(null, false, wp.getConstr(FunctionLib.class));
        glob.define(null, false, wp.getConstr(ArrayLib.class));

        glob.define(null, false, wp.getConstr(BooleanLib.class));
        glob.define(null, false, wp.getConstr(NumberLib.class));
        glob.define(null, false, wp.getConstr(StringLib.class));
        glob.define(null, false, wp.getConstr(SymbolLib.class));

        glob.define(null, false, wp.getConstr(PromiseLib.class));
        glob.define(null, false, wp.getConstr(RegExpLib.class));
        glob.define(null, false, wp.getConstr(MapLib.class));
        glob.define(null, false, wp.getConstr(SetLib.class));

        glob.define(null, false, wp.getConstr(ErrorLib.class));
        glob.define(null, false, wp.getConstr(SyntaxErrorLib.class));
        glob.define(null, false, wp.getConstr(TypeErrorLib.class));
        glob.define(null, false, wp.getConstr(RangeErrorLib.class));

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

        env.add(Environment.REGEX_CONSTR, wp.getConstr(RegExpLib.class));
        Values.setPrototype(Environment.empty(), wp.getProto(ObjectLib.class), null);

        env.add(NativeWrapperProvider.KEY, wp);
        env.add(GlobalScope.KEY, glob);

        return env;
    }
}
