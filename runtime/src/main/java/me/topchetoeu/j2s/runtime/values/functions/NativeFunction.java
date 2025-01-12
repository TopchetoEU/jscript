package me.topchetoeu.j2s.runtime.values.functions;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.runtime.values.Value;

public final class NativeFunction extends FunctionValue {
	public static interface NativeFunctionRunner {
		Value run(Arguments args);
	}

	public final NativeFunctionRunner action;

	@Override protected Value onApply(Environment env, Value self, Value... args) {
		return action.run(new Arguments(env, false, self, args));
	}
	@Override protected Value onConstruct(Environment env, Value target, Value... args) {
		return action.run(new Arguments(env, true, target, args));
	}

	public NativeFunction(String name, NativeFunctionRunner action) {
		super(name, 0);
		this.action = action;
	}
	public NativeFunction(NativeFunctionRunner action) {
		super("", 0);
		this.action = action;
	}
}
