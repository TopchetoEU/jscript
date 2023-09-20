package me.topchetoeu.jscript.polyfills;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;

public class FunctionPolyfill {
    @Native(raw=true) public static Object apply(Context ctx, Object func, Object[] args) throws InterruptedException {
        var thisArg = args.length > 0 ? args[0] : null;
        var _args = args.length > 1 ? args[1] : null;

        if (!(_args instanceof ArrayValue)) throw EngineException.ofError("Expected arguments to be an array.");
        if (!(func instanceof FunctionValue)) throw EngineException.ofError("Expected this to be a function.");

        return ((FunctionValue)func).call(ctx, thisArg, ((ArrayValue)_args).toArray());
    }
    @Native(raw=true) public static Object call(Context ctx, Object func, Object[] args) throws InterruptedException {
        var thisArg = args.length > 0 ? args[0] : null;
        var _args = new Object[args.length > 1 ? args.length - 1 : 0];

        if (!(func instanceof FunctionValue)) throw EngineException.ofError("Expected this to be a function.");

        if (_args.length != 0) System.arraycopy(args, 1, _args, 0, _args.length);

        return ((FunctionValue)func).call(ctx, thisArg, _args);
    }
    @Native(raw=true) public static Object bind(Context ctx, Object func, Object[] args) {
        var thisArg = args.length > 0 ? args[0] : null;
        var _args = new Object[args.length > 1 ? args.length - 1 : 0];
        FunctionValue _func = (FunctionValue)func;

        if (!(func instanceof FunctionValue)) throw EngineException.ofError("Expected this to be a function.");

        if (_args.length != 0) System.arraycopy(args, 1, _args, 0, _args.length);

        return new NativeFunction(_func.name + " (bound)", (callCtx, _0, callArgs) -> {
            var resArgs = new Object[_args.length + callArgs.length];
            System.arraycopy(_args, 0, resArgs, 0, _args.length);
            System.arraycopy(callArgs, 0, resArgs, _args.length, resArgs.length - _args.length);

            return _func.call(ctx, thisArg, resArgs);
        });
    }
    @Native(raw=true) public static String toString(Context ctx, Object func, Object[] args) {
        return "function (...) { ... }";
    }

    @Native public static FunctionValue async(FunctionValue func) {
        return new AsyncFunctionPolyfill(func);
    }
    @Native public static FunctionValue asyncGenerator(FunctionValue func) {
        return new AsyncGeneratorPolyfill(func);
    }
    @Native public static FunctionValue generator(FunctionValue func) {
        return new GeneratorPolyfill(func);
    }
}
