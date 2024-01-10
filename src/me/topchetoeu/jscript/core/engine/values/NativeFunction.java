package me.topchetoeu.jscript.core.engine.values;

import me.topchetoeu.jscript.core.engine.Context;
import me.topchetoeu.jscript.utils.interop.Arguments;

public class NativeFunction extends FunctionValue {
    public static interface NativeFunctionRunner {
        Object run(Arguments args);
    }

    public final NativeFunctionRunner action;

    @Override
    public Object call(Context ctx, Object thisArg, Object ...args) {
        return action.run(new Arguments(ctx, thisArg, args));
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
