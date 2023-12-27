package me.topchetoeu.jscript.lib;

import java.util.Map;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.frame.Runners;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;

@Native("AsyncGenerator") public class AsyncGeneratorLib {
    @Native("@@Symbol.typeName") public final String name = "AsyncGenerator";
    private int state = 0;
    private boolean done = false;
    private PromiseLib currPromise;
    public CodeFrame frame;

    private void next(Context ctx, Object inducedValue, Object inducedReturn, Object inducedError) {
        if (done) {
            if (inducedError != Runners.NO_RETURN) throw new EngineException(inducedError);
            currPromise.fulfill(ctx, new ObjectValue(ctx, Map.of(
                "done", true,
                "value", inducedReturn == Runners.NO_RETURN ? null : inducedReturn
            )));
            return;
        }

        Object res = null;
        state = 0;

        while (state == 0) {
            try {
                res = frame.next(inducedValue, inducedReturn, inducedError == Runners.NO_RETURN ? null : new EngineException(inducedError));
                inducedValue = inducedReturn = inducedError = Runners.NO_RETURN;
                if (res != Runners.NO_RETURN) {
                    var obj = new ObjectValue();
                    obj.defineProperty(ctx, "done", true);
                    obj.defineProperty(ctx, "value", res);
                    currPromise.fulfill(ctx, obj);
                    break;
                }
            }
            catch (EngineException e) {
                currPromise.reject(ctx, e.value);
                break;
            }
        }

        if (state == 1) {
            PromiseLib.then(ctx, frame.pop(), new NativeFunction(this::fulfill), new NativeFunction(this::reject));
        }
        else if (state == 2) {
            var obj = new ObjectValue();
            obj.defineProperty(ctx, "done", false);
            obj.defineProperty(ctx, "value", frame.pop());
            currPromise.fulfill(ctx, obj);
        }
    }

    @Override
    public String toString() {
        if (done) return "Generator [closed]";
        if (state != 0) return "Generator [suspended]";
        return "Generator [running]";
    }

    public Object fulfill(Context ctx, Object thisArg, Object ...args) {
        next(ctx, args.length > 0 ? args[0] : null, Runners.NO_RETURN, Runners.NO_RETURN);
        return null;
    }
    public Object reject(Context ctx, Object thisArg, Object ...args) {
        next(ctx, Runners.NO_RETURN, args.length > 0 ? args[0] : null, Runners.NO_RETURN);
        return null;
    }

    @Native
    public PromiseLib next(Context ctx, Object ...args) {
        this.currPromise = new PromiseLib();
        if (args.length == 0) next(ctx, Runners.NO_RETURN, Runners.NO_RETURN, Runners.NO_RETURN);
        else next(ctx, args[0], Runners.NO_RETURN, Runners.NO_RETURN);
        return this.currPromise;
    }
    @Native("throw")
    public PromiseLib _throw(Context ctx, Object error) {
        this.currPromise = new PromiseLib();
        next(ctx, Runners.NO_RETURN, Runners.NO_RETURN, error);
        return this.currPromise;
    }
    @Native("return")
    public PromiseLib _return(Context ctx, Object value) {
        this.currPromise = new PromiseLib();
        next(ctx, Runners.NO_RETURN, value, Runners.NO_RETURN);
        return this.currPromise;
    }


    public Object await(Context ctx, Object thisArg, Object[] args) {
        this.state = 1;
        return args.length > 0 ? args[0] : null;
    }
    public Object yield(Context ctx, Object thisArg, Object[] args) {
        this.state = 2;
        return args.length > 0 ? args[0] : null;
    }
}