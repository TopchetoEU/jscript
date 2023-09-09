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

    public String toString(Context ctx) throws InterruptedException {
        var ss = new StringBuilder();
        try {
            ss.append(Values.toString(ctx, value)).append('\n');
        }
        catch (EngineException e) {
            ss.append("[Error while stringifying]\n");
        }
        for (var line : stackTrace) {
            ss.append("    ").append(line).append('\n');
        }
        if (cause != null) ss.append("Caused by ").append(cause.toString(ctx)).append('\n');
        ss.deleteCharAt(ss.length() - 1);
        return ss.toString();
    }

    private static Object err(String msg, PlaceholderProto proto) {
        var res = new ObjectValue(proto);
        res.defineProperty(null, "message", msg);
        return res;
    }

    public EngineException(Object error) {
        super(error == null ? "null" : error.toString());

        this.value = error;
        this.cause = null;
    }

    public static EngineException ofError(String msg) {
        return new EngineException(err(msg, PlaceholderProto.ERROR));
    }
    public static EngineException ofSyntax(SyntaxException e) {
        return new EngineException(err(e.msg, PlaceholderProto.SYNTAX_ERROR)).add(null, e.loc);
    }
    public static EngineException ofSyntax(String msg) {
        return new EngineException(err(msg, PlaceholderProto.SYNTAX_ERROR));
    }
    public static EngineException ofType(String msg) {
        return new EngineException(err(msg, PlaceholderProto.TYPE_ERROR));
    }
    public static EngineException ofRange(String msg) {
        return new EngineException(err(msg, PlaceholderProto.RANGE_ERROR));
    }
}
