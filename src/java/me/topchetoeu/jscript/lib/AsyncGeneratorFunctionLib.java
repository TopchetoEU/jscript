package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.core.Context;
import me.topchetoeu.jscript.core.Frame;
import me.topchetoeu.jscript.core.values.CodeFunction;
import me.topchetoeu.jscript.core.values.FunctionValue;
import me.topchetoeu.jscript.core.values.NativeFunction;
import me.topchetoeu.jscript.core.exceptions.EngineException;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("AsyncGeneratorFunction")
public class AsyncGeneratorFunctionLib extends FunctionValue {
    public final CodeFunction func;

    @Override
    public Object call(Context ctx, Object thisArg, Object ...args) {
        var handler = new AsyncGeneratorLib();

        var newArgs = new Object[args.length + 2];
        newArgs[0] = new NativeFunction("await", handler::await);
        newArgs[1] = new NativeFunction("yield", handler::yield);
        System.arraycopy(args, 0, newArgs, 2, args.length);

        handler.frame = new Frame(ctx, thisArg, newArgs, func);
        return handler;
    }

    public AsyncGeneratorFunctionLib(CodeFunction func) {
        super(func.name, func.length);
        if (!(func instanceof CodeFunction)) throw EngineException.ofType("Return value of argument must be a js function.");
        this.func = func;
    }
}
