package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.core.engine.Context;
import me.topchetoeu.jscript.core.engine.frame.CodeFrame;
import me.topchetoeu.jscript.core.engine.values.CodeFunction;
import me.topchetoeu.jscript.core.engine.values.FunctionValue;
import me.topchetoeu.jscript.core.engine.values.NativeFunction;
import me.topchetoeu.jscript.core.exceptions.EngineException;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("AsyncGeneratorFunction")
public class AsyncGeneratorFunctionLib extends FunctionValue {
    public final FunctionValue factory;

    @Override
    public Object call(Context ctx, Object thisArg, Object ...args) {
        var handler = new AsyncGeneratorLib();
        var func = factory.call(ctx, thisArg,
            new NativeFunction("await", handler::await),
            new NativeFunction("yield", handler::yield)
        );
        if (!(func instanceof CodeFunction)) throw EngineException.ofType("Return value of argument must be a js function.");
        handler.frame = new CodeFrame(ctx, thisArg, args, (CodeFunction)func);
        return handler;
    }

    public AsyncGeneratorFunctionLib(FunctionValue factory) {
        super(factory.name, factory.length);
        this.factory = factory;
    }
}
