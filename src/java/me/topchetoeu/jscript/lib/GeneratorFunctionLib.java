package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.core.Context;
import me.topchetoeu.jscript.core.Frame;
import me.topchetoeu.jscript.core.values.CodeFunction;
import me.topchetoeu.jscript.core.values.FunctionValue;
import me.topchetoeu.jscript.core.values.NativeFunction;
import me.topchetoeu.jscript.core.exceptions.EngineException;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("GeneratorFunction")
public class GeneratorFunctionLib extends FunctionValue {
    public final FunctionValue factory;

    @Override public Object call(Context ctx, Object thisArg, Object ...args) {
        var handler = new GeneratorLib();
        var func = factory.call(ctx, thisArg, new NativeFunction("yield", handler::yield));
        if (!(func instanceof CodeFunction)) throw EngineException.ofType("Return value of argument must be a js function.");
        handler.frame = new Frame(ctx, thisArg, args, (CodeFunction)func);
        return handler;
    }

    public GeneratorFunctionLib(FunctionValue factory) {
        super(factory.name, factory.length);
        this.factory = factory;
    }
}
