package me.topchetoeu.jscript.exceptions;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.engine.values.ObjectValue.PlaceholderProto;

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

    public String toString(Context ctx) {
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
        return new EngineException(err(null, e.msg, PlaceholderProto.SYNTAX_ERROR)).add(null, e.loc);
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
