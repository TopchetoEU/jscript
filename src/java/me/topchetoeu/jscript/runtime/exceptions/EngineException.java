package me.topchetoeu.jscript.runtime.exceptions;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.values.ObjectValue;
import me.topchetoeu.jscript.runtime.values.Values;
import me.topchetoeu.jscript.runtime.values.ObjectValue.PlaceholderProto;

public class EngineException extends RuntimeException {
    public static class StackElement {
        public final Location location;
        public final String name;
        public final Environment ext;

        public boolean visible() {
            return ext == null || !ext.get(Environment.HIDE_STACK, false);
        }
        public String toString() {
            var res = "";
            var loc = location;

            if (loc != null) res += "at " + loc.toString() + " ";
            if (name != null && !name.equals("")) res += "in " + name + " ";

            return res.trim();
        }

        public StackElement(Environment ext, Location location, String name) {
            if (name != null) name = name.trim();
            if (name.equals("")) name = null;

            if (ext == null) this.ext = null;
            else this.ext = ext;

            this.location = location;
            this.name = name;
        }
    }

    public final Object value;
    public EngineException cause;
    public Environment env = null;
    public final List<StackElement> stackTrace = new ArrayList<>();

    public EngineException add(Environment env, String name, Location location) {
        var el = new StackElement(env, location, name);
        if (el.name == null && el.location == null) return this;
        setEnvironment(env);
        stackTrace.add(el);
        return this;
    }
    public EngineException setCause(EngineException cause) {
        this.cause = cause;
        return this;
    }
    public EngineException setEnvironment(Environment env) {
        if (this.env == null) this.env = env;
        return this;
    }

    public String toString(Environment ext) {
        var ss = new StringBuilder();
        try {
            ss.append(Values.toString(ext, value)).append('\n');
        }
        catch (EngineException e) {
            ss.append("[Error while stringifying]\n");
        }
        for (var line : stackTrace) {
            if (line.visible()) ss.append("    ").append(line.toString()).append("\n");
        }
        if (cause != null) ss.append("Caused by ").append(cause.toString(ext)).append('\n');
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
