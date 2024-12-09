package me.topchetoeu.jscript.runtime.values.functions;


import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.primitives.UserValue;

public class Arguments {
	public final Value self;
	public final Value[] args;
	public final Environment env;
	public final boolean isNew;

	public int n() {
		return args.length;
	}

	public boolean has(int i) {
		return i == -1 || i >= 0 && i < args.length;
	}

	public Value self() {
		return get(-1);
	}
	@SuppressWarnings("unchecked")
	public <T> T self(Class<T> clazz) {
		if (self instanceof UserValue user && clazz.isInstance(user.value)) return (T)user.value;
		else return null;
	}
	public Value get(int i) {
		if (i >= args.length || i < -1) return Value.UNDEFINED;
		else if (i == -1) return self;
		else return args[i];
	}
	public Value getOrDefault(int i, Value def) {
		if (i < -1 || i >= args.length) return def;
		else return get(i);
	}

	public Arguments(Environment env, boolean isNew, Value thisArg, Value... args) {
		this.env = env;
		this.args = args;
		this.self = thisArg;
		this.isNew = isNew;
	}
}
