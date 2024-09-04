package me.topchetoeu.jscript.runtime.values;

import me.topchetoeu.jscript.runtime.Context;
import me.topchetoeu.jscript.runtime.Extensions;
import me.topchetoeu.jscript.utils.interop.Arguments;

public class NativeFunction extends FunctionValue {
    public static interface NativeFunctionRunner {
        Object run(Arguments args);
    }

    public final NativeFunctionRunner action;

    @Override
    public Object call(Extensions ext, Object thisArg, Object ...args) {
        return action.run(new Arguments(Context.of(ext), thisArg, args));
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
