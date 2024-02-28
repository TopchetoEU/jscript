package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.core.values.ArrayValue;
import me.topchetoeu.jscript.core.values.CodeFunction;
import me.topchetoeu.jscript.core.values.FunctionValue;
import me.topchetoeu.jscript.core.values.NativeFunction;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeTarget;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("Function")
public class FunctionLib {
    @Expose public static Object __apply(Arguments args) {
        return args.self(FunctionValue.class).call(args.ctx, args.get(0), args.convert(1, ArrayValue.class).toArray());
    }
    @Expose public static Object __call(Arguments args) {
        return args.self(FunctionValue.class).call(args.ctx, args.get(0), args.slice(1).args);
    }
    @Expose public static FunctionValue __bind(Arguments args) {
        var self = args.self(FunctionValue.class);
        var thisArg = args.get(0);
        var bindArgs = args.slice(1).args;

        return new NativeFunction(self.name + " (bound)", callArgs -> {
            Object[] resArgs;

            if (args.n() == 0) resArgs = bindArgs;
            else {
                resArgs = new Object[bindArgs.length + callArgs.n()];
                System.arraycopy(bindArgs, 0, resArgs, 0, bindArgs.length);
                System.arraycopy(callArgs.args, 0, resArgs, bindArgs.length, callArgs.n());
            }

            return self.call(callArgs.ctx, thisArg, resArgs);
        });
    }
    @Expose public static String __toString(Arguments args) {
        return args.self.toString();
    }

    @Expose(target = ExposeTarget.STATIC)
    public static FunctionValue __async(Arguments args) {
        return new AsyncFunctionLib(args.convert(0, FunctionValue.class));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static FunctionValue __asyncGenerator(Arguments args) {
        return new AsyncGeneratorFunctionLib(args.convert(0, CodeFunction.class));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static FunctionValue __generator(Arguments args) {
        return new GeneratorFunctionLib(args.convert(0, CodeFunction.class));
    }
}
