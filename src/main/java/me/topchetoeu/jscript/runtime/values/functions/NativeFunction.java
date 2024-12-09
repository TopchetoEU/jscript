package me.topchetoeu.jscript.runtime.values.functions;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.Value;

public final class NativeFunction extends FunctionValue {
    public static interface NativeFunctionRunner {
        Value run(Arguments args);
    }

    public final NativeFunctionRunner action;

    @Override public Value onCall(Environment env, boolean isNew, Value self, Value ...args) {
        return action.run(new Arguments(env, isNew, self, args));
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
