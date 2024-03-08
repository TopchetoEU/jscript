package me.topchetoeu.jscript.lib;

import java.util.Map;

import me.topchetoeu.jscript.lib.PromiseLib.Handle;
import me.topchetoeu.jscript.runtime.Context;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.ObjectValue;
import me.topchetoeu.jscript.runtime.values.Values;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("AsyncGenerator")
public class AsyncGeneratorLib {
    private int state = 0;
    private boolean done = false;
    private PromiseLib currPromise;
    public Frame frame;

    private void next(Context ctx, Object inducedValue, Object inducedReturn, EngineException inducedError) {
        if (done) {
            if (inducedError != null) throw inducedError;
            currPromise.fulfill(ctx, new ObjectValue(ctx, Map.of(
                "done", true,
                "value", inducedReturn == Values.NO_RETURN ? null : inducedReturn
            )));
            return;
        }

        Object res = null;
        state = 0;

        frame.onPush();
        while (state == 0) {
            try {
                res = frame.next(inducedValue, inducedReturn, inducedError);
                inducedValue = inducedReturn = Values.NO_RETURN;
                inducedError = null;

                if (res != Values.NO_RETURN) {
                    var obj = new ObjectValue();
                    obj.defineProperty(ctx, "done", true);
                    obj.defineProperty(ctx, "value", res);
                    currPromise.fulfill(ctx, obj);
                    break;
                }
            }
            catch (EngineException e) {
                currPromise.reject(ctx, e);
                break;
            }
        }
        frame.onPop();

        if (state == 1) {
            PromiseLib.handle(ctx, frame.pop(), new Handle() {
                @Override public void onFulfil(Object val) {
                    next(ctx, val, Values.NO_RETURN, null);
                }
                @Override public void onReject(EngineException err) {
                    next(ctx, Values.NO_RETURN, Values.NO_RETURN, err);
                }
            }.defer(ctx));
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

    public Object await(Arguments args) {
        this.state = 1;
        return args.get(0);
    }
    public Object yield(Arguments args) {
        this.state = 2;
        return args.get(0);
    }

    @Expose public PromiseLib __next(Arguments args) {
        this.currPromise = new PromiseLib();
        if (args.has(0)) next(args.ctx, args.get(0), Values.NO_RETURN, null);
        else next(args.ctx, Values.NO_RETURN, Values.NO_RETURN, null);
        return this.currPromise;
    }
    @Expose public PromiseLib __return(Arguments args) {
        this.currPromise = new PromiseLib();
        next(args.ctx, Values.NO_RETURN, args.get(0), null);
        return this.currPromise;
    }
    @Expose public PromiseLib __throw(Arguments args) {
        this.currPromise = new PromiseLib();
        next(args.ctx, Values.NO_RETURN, Values.NO_RETURN, new EngineException(args.get(0)).setCtx(args.ctx));
        return this.currPromise;
    }
}