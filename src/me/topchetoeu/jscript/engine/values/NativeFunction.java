package me.topchetoeu.jscript.engine.values;

import me.topchetoeu.jscript.engine.CallContext;

public class NativeFunction extends FunctionValue {
    public static interface NativeFunctionRunner {
        Object run(CallContext ctx, Object thisArg, Object[] args) throws InterruptedException;
    }

    public final NativeFunctionRunner action;

    @Override
    public Object call(CallContext ctx, Object thisArg, Object... args) throws InterruptedException {
        return action.run(ctx, thisArg, args);
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
