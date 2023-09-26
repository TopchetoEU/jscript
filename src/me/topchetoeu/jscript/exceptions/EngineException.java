package me.topchetoeu.jscript.exceptions;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;

public class EngineException extends RuntimeException {
    public final Object value;
    public EngineException cause;
    public Context ctx = null;
    public final List<String> stackTrace = new ArrayList<>();

    public EngineException add(String name, Location location) {
        var res = "";

        if (location != null) res += "at " + location.toString() + " ";
        if (name != null && !name.equals("")) res += "in " + name + " ";

        this.stackTrace.add(res.trim());
        return this;
    }
    public EngineException setCause(EngineException cause) {
        this.cause = cause;
        return this;
    }
    public EngineException setContext(Context ctx) {
        this.ctx = ctx;
        return this;
    }

    public String toString(Context ctx) throws InterruptedException {
        var ss = new StringBuilder();
        try {
            ss.append(Values.toString(ctx, value)).append('\n');
        }
        catch (EngineException e) {
            ss.append("[Error while stringifying]\n");
        }
        // for (var line : stackTrace) {
        //     ss.append("    ").append(line).append('\n');
        // }
        // if (cause != null) ss.append("Caused by ").append(cause.toString(ctx)).append('\n');
        ss.deleteCharAt(ss.length() - 1);
        return ss.toString();
    }

    private static Object err(Context ctx, String type, String name, String msg) throws InterruptedException {
        try {
            var proto = ctx.env.proto(type);
            var constr = Values.getMember(ctx, proto, "constructor");

            if (constr instanceof FunctionValue) {
                var res = Values.callNew(ctx, constr, msg);
                if (name != null) Values.setMember(ctx, res, "name", name);
                return res;
            }
        }
        catch (IllegalArgumentException e) { }

        return name + ": " + msg;
    }

    public EngineException(Object error) {
        super(error == null ? "null" : error.toString());

        this.value = error;
        this.cause = null;
    }

    public static EngineException ofError(Context ctx, String name, String msg) throws InterruptedException {
        return new EngineException(err(ctx, "error", name, msg));
    }
    public static EngineException ofError(Context ctx, String msg) throws InterruptedException {
        return new EngineException(err(ctx, "error", null, msg));
    }
    public static EngineException ofSyntax(Context ctx, SyntaxException e) throws InterruptedException {
        return new EngineException(err(ctx, "syntaxErr", null, e.msg)).add(null, e.loc);
    }
    public static EngineException ofSyntax(Context ctx, String msg) throws InterruptedException {
        return new EngineException(err(ctx, "syntaxErr", null, msg));
    }
    public static EngineException ofType(Context ctx, String msg) throws InterruptedException {
        return new EngineException(err(ctx, "typeErr", null, msg));
    }
    public static EngineException ofRange(Context ctx, String msg) throws InterruptedException {
        return new EngineException(err(ctx, "rangeErr", null, msg));
    }
}
