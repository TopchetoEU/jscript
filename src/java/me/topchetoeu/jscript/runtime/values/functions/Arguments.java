package me.topchetoeu.jscript.runtime.values.functions;


import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.primitives.VoidValue;

public class Arguments {
    public final Value self;
    public final Value[] args;
    public final Environment env;

    public int n() {
        return args.length;
    }

    public boolean has(int i) {
        return i == -1 || i >= 0 && i < args.length;
    }

    public Value self() {
        return get(-1);
    }
    public Value get(int i) {
        if (i >= args.length || i < -1) return VoidValue.UNDEFINED;
        else if (i == -1) return self;
        else return args[i];
    }
    public Value getOrDefault(int i, Value def) {
        if (i < -1 || i >= args.length) return def;
        else return get(i);
    }

    public Arguments(Environment env, Value thisArg, Value... args) {
        this.env = env;
        this.args = args;
        this.self = thisArg;
    }
}
