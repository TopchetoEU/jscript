package me.topchetoeu.jscript.core.exceptions;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.Context;
import me.topchetoeu.jscript.core.Environment;
import me.topchetoeu.jscript.core.values.ObjectValue;
import me.topchetoeu.jscript.core.values.Values;
import me.topchetoeu.jscript.core.values.ObjectValue.PlaceholderProto;

public class EngineException extends RuntimeException {
    public static class StackElement {
        public final Location location;
        public final String name;
        public final Context ctx;

        public boolean visible() {
            return ctx == null || !ctx.get(Environment.HIDE_STACK, false);
        }
        public String toString() {
            var res = "";
            var loc = location;

            if (loc != null) res += "at " + loc.toString() + " ";
            if (name != null && !name.equals("")) res += "in " + name + " ";

            return res.trim();
        }

        public StackElement(Context ctx, Location location, String name) {
            if (name != null) name = name.trim();
            if (name.equals("")) name = null;

            if (ctx == null) this.ctx = null;
            else this.ctx = new Context(ctx.environment);
            this.location = location;
            this.name = name;
        }
    }

    public final Object value;
    public EngineException cause;
    public Environment env = null;
    public final List<StackElement> stackTrace = new ArrayList<>();

    public EngineException add(Context ctx, String name, Location location) {
        var el = new StackElement(ctx, location, name);
        if (el.name == null && el.location == null) return this;
        setCtx(ctx);
        stackTrace.add(el);
        return this;
    }
    public EngineException setCause(EngineException cause) {
        this.cause = cause;
        return this;
    }
    public EngineException setCtx(Context ctx) {
        if (this.env == null) this.env = ctx.environment;
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
