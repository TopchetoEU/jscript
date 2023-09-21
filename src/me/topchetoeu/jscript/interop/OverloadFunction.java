package me.topchetoeu.jscript.interop;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.ConvertException;
import me.topchetoeu.jscript.exceptions.EngineException;

public class OverloadFunction extends FunctionValue {
    public final List<Overload> overloads = new ArrayList<>();

    public Object call(Context ctx, Object thisArg, Object ...args) throws InterruptedException {
        loop: for (var overload : overloads) {
            Object[] newArgs = new Object[overload.params.length];

            boolean consumesEngine = overload.params.length > 0 && overload.params[0] == Context.class;
            int start = (consumesEngine ? 1 : 0) + (overload.passThis ? 1 : 0);
            int end = overload.params.length - (overload.variadic ? 1 : 0);

            for (var i = start; i < end; i++) {
                Object val;

                if (i - start >= args.length) val = null;
                else val = args[i - start];

                try {
                    newArgs[i] = Values.convert(ctx, val, overload.params[i]);
                }
                catch (ConvertException e) {
                    if (overloads.size() > 1) continue loop;
                    else throw EngineException.ofType(String.format("Argument %d can't be converted from %s to %s", i - start, e.source, e.target));
                }
            }

            if (overload.variadic) {
                var type = overload.params[overload.params.length - 1].getComponentType();
                var n = Math.max(args.length - end + start, 0);
                Object varArg = Array.newInstance(type, n);

                for (var i = 0; i < n; i++) {
                    try {
                        Array.set(varArg, i, Values.convert(ctx, args[i + end - start], type));
                    }
                    catch (ConvertException e) {
                        if (overloads.size() > 1) continue loop;
                        else throw EngineException.ofType(String.format("Element in variadic argument can't be converted from %s to %s", e.source, e.target));
                    }
                }

                newArgs[newArgs.length - 1] = varArg;
            }

            var thisArgType = overload.passThis ? overload.params[consumesEngine ? 1 : 0] : overload.thisArg;
            Object _this = thisArgType == null ? null : Values.convert(ctx, thisArg, thisArgType);

            if (consumesEngine) newArgs[0] = ctx;
            if (overload.passThis) newArgs[consumesEngine ? 1 : 0] = _this;

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
