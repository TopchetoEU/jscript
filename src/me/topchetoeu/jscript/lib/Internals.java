package me.topchetoeu.jscript.lib;

import java.io.IOException;

import me.topchetoeu.jscript.Buffer;
import me.topchetoeu.jscript.Reading;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.scope.GlobalScope;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeGetter;
import me.topchetoeu.jscript.modules.ModuleRepo;
import me.topchetoeu.jscript.parsing.Parsing;

public class Internals {
    @Native public static Object require(Context ctx, String name) {
        var repo = ModuleRepo.get(ctx);

        if (repo != null) {
            var res = repo.getModule(ctx, ModuleRepo.cwd(ctx), name);
            res.load(ctx);
            return res.value();
        }

        else throw EngineException.ofError("Modules are not supported.");
    }

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

    @Native public static Thread setTimeout(Context ctx, FunctionValue func, int delay, Object ...args) {
        var thread = new Thread(() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            try {
                Thread.sleep(ms, ns);
            }
            catch (InterruptedException e) { return; }

            ctx.engine.pushMsg(false, ctx.environment(), func, null, args);
        });
        thread.start();

        return thread;
    }
    @Native public static Thread setInterval(Context ctx, FunctionValue func, int delay, Object ...args) {
        var thread = new Thread(() -> {
            var ms = (long)delay;
            var ns = (int)((delay - ms) * 10000000);

            while (true) {
                try {
                    Thread.sleep(ms, ns);
                }
                catch (InterruptedException e) { return; }

                ctx.engine.pushMsg(false, ctx.environment(), func, null, args);
            }
        });

        return thread;
    }

    @Native public static void clearTimeout(Context ctx, Thread t) {
        t.interrupt();
    }
    @Native public static void clearInterval(Context ctx, Thread t) {
        t.interrupt();
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
    private static final String HEX = "0123456789ABCDEF";

    private static String encodeUriAny(String str, String keepAlphabet) {
        if (str == null) str = "undefined";

        var bytes = str.getBytes();
        var sb = new StringBuilder(bytes.length);

        for (byte c : bytes) {
            if (Parsing.isAlphanumeric((char)c) || Parsing.isAny((char)c, keepAlphabet)) sb.append((char)c);
            else {
                sb.append('%');
                sb.append(HEX.charAt(c / 16));
                sb.append(HEX.charAt(c % 16));
            }
        }

        return sb.toString();
    }
    private static String decodeUriAny(String str, String keepAlphabet) {
        if (str == null) str = "undefined";

        var res = new Buffer();
        var bytes = str.getBytes();

        for (var i = 0; i < bytes.length; i++) {
            var c = bytes[i];
            if (c == '%') {
                if (i >= bytes.length - 2) throw EngineException.ofError("URIError", "URI malformed.");
                var b = Parsing.fromHex((char)bytes[i + 1]) * 16 | Parsing.fromHex((char)bytes[i + 2]);
                if (!Parsing.isAny((char)b, keepAlphabet)) {
                    i += 2;
                    res.append((byte)b);
                    continue;
                }
            }
            res.append(c);
        }

        return new String(res.data());
    }

    @Native public static String encodeURIComponent(String str) {
        return encodeUriAny(str, ".-_!~*'()");
    }
    @Native public static String decodeURIComponent(String str) {
        return decodeUriAny(str, "");
    }
    @Native public static String encodeURI(String str) {
        return encodeUriAny(str, ";,/?:@&=+$#.-_!~*'()");
    }
    @Native public static String decodeURI(String str) {
        return decodeUriAny(str, ",/?:@&=+$#.");
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

        wp.getProto(ObjectLib.class).setPrototype(null, null);
        env.add(Environment.REGEX_CONSTR, wp.getConstr(RegExpLib.class));

        return env;
    }
}
