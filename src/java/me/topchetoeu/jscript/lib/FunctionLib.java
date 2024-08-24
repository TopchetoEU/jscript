package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.runtime.Compiler;
import me.topchetoeu.jscript.runtime.scope.ValueVariable;
import me.topchetoeu.jscript.runtime.values.ArrayValue;
import me.topchetoeu.jscript.runtime.values.CodeFunction;
import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.NativeFunction;
import me.topchetoeu.jscript.runtime.values.Values;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeConstructor;
import me.topchetoeu.jscript.utils.interop.ExposeTarget;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("Function")
public class FunctionLib {
    private static int i;

    @Expose public static Object __apply(Arguments args) {
        return args.self(FunctionValue.class).call(args.env, args.get(0), args.convert(1, ArrayValue.class).toArray());
    }
    @Expose public static Object __call(Arguments args) {
        return args.self(FunctionValue.class).call(args.env, args.get(0), args.slice(1).args);
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

            return self.call(callArgs.env, thisArg, resArgs);
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

    @ExposeConstructor
    public static Object __constructor(Arguments args) {
        var parts = args.convert(String.class);
        if (parts.length == 0) parts = new String[] { "" };

        var src = "return function(";

        for (var i = 0; i < parts.length - 1; i++) {
            if (i != 0) src += ",";
            src += parts[i];
        }

        src += "){" + parts[parts.length - 1] + "}";

        var body = Compiler.get(args.env).compile(new Filename("jscript", "func/" + i++), src);
        var func = new CodeFunction(args.env, "", body, new ValueVariable[0]);
        return Values.call(args.env, func, null);
    }
}
