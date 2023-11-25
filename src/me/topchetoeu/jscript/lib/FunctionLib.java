package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;

@Native("Function") public class FunctionLib {
    @Native(thisArg = true) public static Object location(Context ctx, FunctionValue func) {
        if (func instanceof CodeFunction) return ((CodeFunction)func).loc().toString();
        else return Location.INTERNAL.toString();
    }
    @Native(thisArg = true) public static Object apply(Context ctx, FunctionValue func, Object thisArg, ArrayValue args) {
        return func.call(ctx, thisArg, args.toArray());
    }
    @Native(thisArg = true) public static Object call(Context ctx, FunctionValue func, Object thisArg, Object... args) {
        if (!(func instanceof FunctionValue)) throw EngineException.ofError("Expected this to be a function.");

        return func.call(ctx, thisArg, args);
    }
    @Native(thisArg = true) public static FunctionValue bind(FunctionValue func, Object thisArg, Object... args) {
        if (!(func instanceof FunctionValue)) throw EngineException.ofError("Expected this to be a function.");

        return new NativeFunction(func.name + " (bound)", (callCtx, _0, callArgs) -> {
            Object[] resArgs;

            if (args.length == 0) resArgs = callArgs;
            else {
                resArgs = new Object[args.length + callArgs.length];
                System.arraycopy(args, 0, resArgs, 0, args.length);
                System.arraycopy(callArgs, 0, resArgs, args.length, callArgs.length);
            }

            return func.call(callCtx, thisArg, resArgs);
        });
    }
    @Native(thisArg = true) public static String toString(Context ctx, Object func) {
        return func.toString();
    }

    @Native public static FunctionValue async(FunctionValue func) {
        return new AsyncFunctionLib(func);
    }
    @Native public static FunctionValue asyncGenerator(FunctionValue func) {
        return new AsyncGeneratorFunctionLib(func);
    }
    @Native public static FunctionValue generator(FunctionValue func) {
        return new GeneratorFunctionLib(func);
    }
}
