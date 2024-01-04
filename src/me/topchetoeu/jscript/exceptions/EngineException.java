package me.topchetoeu.jscript.exceptions;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Engine;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.debug.DebugContext;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.engine.values.ObjectValue.PlaceholderProto;

public class EngineException extends RuntimeException {
    public static class StackElement {
        public final Location location;
        public final String function;
        public final Context ctx;

        public boolean visible() {
            return ctx == null || !ctx.get(Environment.HIDE_STACK, false);
        }
        public String toString() {
            var res = "";
            var loc = location;

            if (loc != null && ctx != null && ctx.engine != null) loc = DebugContext.get(ctx).mapToCompiled(loc);

            if (loc != null) res += "at " + loc.toString() + " ";
            if (function != null && !function.equals("")) res += "in " + function + " ";

            return res.trim();
        }

        public StackElement(Context ctx, Location location, String function) {
            if (function != null) function = function.trim();
            if (function.equals("")) function = null;

            if (ctx == null) this.ctx = null;
            else this.ctx = new Context(ctx.engine, ctx.environment);
            this.location = location;
            this.function = function;
        }
    }

    public final Object value;
    public EngineException cause;
    public Environment env = null;
    public Engine engine = null;
    public final List<StackElement> stackTrace = new ArrayList<>();

    public EngineException add(Context ctx, String name, Location location) {
        var el = new StackElement(ctx, location, name);
        if (el.function == null && el.location == null) return this;
        setCtx(ctx.environment, ctx.engine);
        stackTrace.add(el);
        return this;
    }
    public EngineException setCause(EngineException cause) {
        this.cause = cause;
        return this;
    }
    public EngineException setCtx(Environment env, Engine engine) {
        if (this.env == null) this.env = env;
        if (this.engine == null) this.engine = engine;
        return this;
    }
    public EngineException setCtx(Context ctx) {
        if (this.env == null) this.env = ctx.environment;
        if (this.engine == null) this.engine = ctx.engine;
        return this;
    }

    public String toString(Context ctx) {
        var ss = new StringBuilder();
        try {
            ss.append(Values.toString(ctx, value)).append('\n');
        }
        catch (EngineException e) {
            ss.append("[Error while stringifying]\n");
        }
        for (var line : stackTrace) {
            if (line.visible()) ss.append("    ").append(line.toString()).append("\n");
        }
        if (cause != null) ss.append("Caused by ").append(cause.toString(ctx)).append('\n');
        ss.deleteCharAt(ss.length() - 1);
        return ss.toString();
    }

    private static Object err(String name, String msg, PlaceholderProto proto) {
        var res = new ObjectValue(proto);
        if (name != null) res.defineProperty(null, "name", name);
        res.defineProperty(null, "message", msg);
        return res;
    }

    public EngineException(Object error) {
        super(error == null ? "null" : error.toString());

        this.value = error;
        this.cause = null;
    }

    public static EngineException ofError(String name, String msg) {
        return new EngineException(err(name, msg, PlaceholderProto.ERROR));
    }
    public static EngineException ofError(String msg) {
        return new EngineException(err(null, msg, PlaceholderProto.ERROR));
    }
    public static EngineException ofSyntax(SyntaxException e) {
        return new EngineException(err(null, e.msg, PlaceholderProto.SYNTAX_ERROR)).add(null, null, e.loc);
    }
    public static EngineException ofSyntax(String msg) {
        return new EngineException(err(null, msg, PlaceholderProto.SYNTAX_ERROR));
    }
    public static EngineException ofType(String msg) {
        return new EngineException(err(null, msg, PlaceholderProto.TYPE_ERROR));
    }
    public static EngineException ofRange(String msg) {
        return new EngineException(err(null, msg, PlaceholderProto.RANGE_ERROR));
    }
}
