package me.topchetoeu.jscript.runtime.exceptions;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue.PrototypeProvider;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.VoidValue;

public class EngineException extends RuntimeException {
    public static class StackElement {
        public final Location location;
        public final String name;
        public final Environment ext;

        public boolean visible() {
            return ext == null || !ext.get(Value.HIDE_STACK, false);
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

    public final Value value;
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

    public String toString(Environment env) {
        var ss = new StringBuilder();
        try {
            ss.append(value.toString(env)).append('\n');
        }
        catch (EngineException e) {
            var name = value.getMember(env, "name");
            var desc = value.getMember(env, "message");

            if (name.isPrimitive() && desc.isPrimitive()) {
                if (name instanceof VoidValue) ss.append("Error: ");
                else ss.append(name.toString(env) + ": ");

                if (desc instanceof VoidValue) ss.append("An error occurred");
                else ss.append(desc.toString(env));

                ss.append("\n");
            }
            else ss.append("[Error while stringifying]\n");
        }
        for (var line : stackTrace) {
            if (line.visible()) ss.append("    ").append(line.toString()).append("\n");
        }
        if (cause != null) ss.append("Caused by ").append(cause.toString(env)).append('\n');
        ss.deleteCharAt(ss.length() - 1);
        return ss.toString();
    }

    private static ObjectValue err(String name, String msg, PrototypeProvider proto) {
        var res = new ObjectValue();
        res.setPrototype(proto);

        if (msg == null) msg = "";

        if (name != null) res.defineOwnMember(Environment.empty(), "name", StringValue.of(name));
        res.defineOwnMember(Environment.empty(), "message", StringValue.of(msg));
        return res;
    }

    public EngineException(Value error) {
        super(error.toReadable(Environment.empty()));

        this.value = error;
        this.cause = null;
    }

    public static EngineException ofError(String name, String msg) {
        return new EngineException(err(name, msg, env -> env.get(Value.ERROR_PROTO)));
    }
    public static EngineException ofError(String msg) {
        return new EngineException(err(null, msg, env -> env.get(Value.ERROR_PROTO)));
    }
    public static EngineException ofSyntax(String msg) {
        return new EngineException(err(null, msg, env -> env.get(Value.SYNTAX_ERR_PROTO)));
    }
    public static EngineException ofType(String msg) {
        return new EngineException(err(null, msg, env -> env.get(Value.TYPE_ERR_PROTO)));
    }
    public static EngineException ofRange(String msg) {
        return new EngineException(err(null, msg, env -> env.get(Value.RANGE_ERR_PROTO)));
    }
}
