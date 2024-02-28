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
    public final CodeFunction func;

    @Override public Object call(Context ctx, Object thisArg, Object ...args) {
        var handler = new GeneratorLib();

        var newArgs = new Object[args.length + 1];
        newArgs[0] = new NativeFunction("yield", handler::yield);
        System.arraycopy(args, 0, newArgs, 1, args.length);

        handler.frame = new Frame(ctx, thisArg, newArgs, func);
        return handler;
    }

    public GeneratorFunctionLib(CodeFunction func) {
        super(func.name, func.length);
        if (!(func instanceof CodeFunction)) throw EngineException.ofType("Return value of argument must be a js function.");
        this.func = func;
    }
}
