package me.topchetoeu.jscript.interop;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;

public class OverloadFunction extends FunctionValue {
    public final List<Overload> overloads = new ArrayList<>();

    public Object call(Context ctx, Object thisArg, Object ...args) throws InterruptedException {
        for (var overload : overloads) {
            boolean consumesEngine = overload.params.length > 0 && overload.params[0] == Context.class;
            int start = consumesEngine ? 1 : 0;
            int end = overload.params.length - (overload.variadic ? 1 : 0);

            Object[] newArgs = new Object[overload.params.length];

            for (var i = start; i < end; i++) {
                Object val;

                if (i - start >= args.length) val = null;
                else val = args[i - start];

                newArgs[i] = Values.convert(ctx, val, overload.params[i]);
            }

            if (overload.variadic) {
                var type = overload.params[overload.params.length - 1].getComponentType();
                var n = Math.max(args.length - end + start, 0);
                Object varArg = Array.newInstance(type, n);

                for (var i = 0; i < n; i++) {
                    Array.set(varArg, i, Values.convert(ctx, args[i + end - start], type));
                }

                newArgs[newArgs.length - 1] = varArg;
            }

            if (consumesEngine) newArgs[0] = ctx;

            Object _this = overload.thisArg == null ? null : Values.convert(ctx, thisArg, overload.thisArg);

            try {
                return Values.normalize(ctx, overload.runner.run(ctx, _this, newArgs));
            }
            catch (InstantiationException e) {
                throw EngineException.ofError("The class may not be instantiated.");
            }
            catch (IllegalAccessException | IllegalArgumentException e) {
                continue;
            }
            catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof EngineException) {
                    throw ((EngineException)e.getTargetException());
                }
                else {
                    throw EngineException.ofError(e.getTargetException().getMessage());
                }
            }
            catch (ReflectiveOperationException e) {
                throw EngineException.ofError(e.getMessage());
            }
            catch (Exception e) {
                throw e;
            }
        }

        throw EngineException.ofType("No overload found for native method.");
    }

    public OverloadFunction add(Overload overload) {
        this.overloads.add(overload);
        return this;
    }

    public OverloadFunction(String name) {
        super(name, 0);
    }
}
