package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.Expose;
import me.topchetoeu.jscript.interop.ExposeTarget;
import me.topchetoeu.jscript.interop.WrapperName;

@WrapperName("Function")
public class FunctionLib {
    @Expose public static Object __location(Arguments args) {
        if (args.self instanceof CodeFunction) return ((CodeFunction)args.self).loc().toString();
        else return Location.INTERNAL.toString();
    }
    @Expose public static Object __apply(Arguments args) {
        return args.self(FunctionValue.class).call(args.ctx, args.get(0), args.convert(1, ArrayValue.class).toArray());
    }
    @Expose public static Object __call(Arguments args) {
        return args.self(FunctionValue.class).call(args.ctx, args.get(0), args.slice(1).args);
    }
    @Expose public static FunctionValue __bind(Arguments args) {
        var self = args.self(FunctionValue.class);
        return new NativeFunction(self.name + " (bound)", callArgs -> {
            Object[] resArgs;

            if (args.n() == 0) resArgs = callArgs.args;
            else {
                resArgs = new Object[args.n() + callArgs.n()];
                System.arraycopy(args.args, 0, resArgs, 0, args.n());
                System.arraycopy(callArgs.args, 0, resArgs, args.n(), callArgs.n());
            }

            return self.call(callArgs.ctx, self, resArgs);
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
        return new AsyncGeneratorFunctionLib(args.convert(0, FunctionValue.class));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static FunctionValue __generator(Arguments args) {
        return new GeneratorFunctionLib(args.convert(0, FunctionValue.class));
    }
}
