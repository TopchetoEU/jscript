package me.topchetoeu.jscript.polyfills;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.InitType;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeInit;

public class FunctionPolyfill {
    @Native(thisArg = true) public static Object apply(Context ctx, FunctionValue func, Object thisArg, ArrayValue args) throws InterruptedException {
        return func.call(ctx, thisArg, args.toArray());
    }
    @Native(thisArg = true) public static Object call(Context ctx, FunctionValue func, Object thisArg, Object... args) throws InterruptedException {
        if (!(func instanceof FunctionValue)) throw EngineException.ofError("Expected this to be a function.");

        return func.call(ctx, thisArg, args);
    }
    @Native(thisArg = true) public static FunctionValue bind(Context ctx, FunctionValue func, Object thisArg, Object... args) throws InterruptedException {
        if (!(func instanceof FunctionValue)) throw EngineException.ofError("Expected this to be a function.");

        return new NativeFunction(func.name + " (bound)", (callCtx, _0, callArgs) -> {
            var resArgs = new Object[args.length + callArgs.length];
            System.arraycopy(args, 0, resArgs, 0, args.length);
            System.arraycopy(callArgs, 0, resArgs, args.length, callArgs.length);

            return func.call(ctx, thisArg, resArgs);
        });
    }
    @Native(thisArg = true) public static String toString(Context ctx, Object func) {
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

    @NativeInit(InitType.PROTOTYPE) public static void init(Environment env, ObjectValue target) {
        target.defineProperty(null, env.symbol("Symbol.typeName"), "Function");
    }
}
