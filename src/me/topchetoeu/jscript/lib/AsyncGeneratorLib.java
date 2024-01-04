package me.topchetoeu.jscript.lib;

import java.util.Map;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.frame.Runners;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.Expose;
import me.topchetoeu.jscript.interop.WrapperName;
import me.topchetoeu.jscript.lib.PromiseLib.Handle;

@WrapperName("AsyncGenerator")
public class AsyncGeneratorLib {
    private int state = 0;
    private boolean done = false;
    private PromiseLib currPromise;
    public CodeFrame frame;

    private void next(Context ctx, Object inducedValue, Object inducedReturn, EngineException inducedError) {
        if (done) {
            if (inducedError != null) throw inducedError;
            currPromise.fulfill(ctx, new ObjectValue(ctx, Map.of(
                "done", true,
                "value", inducedReturn == Runners.NO_RETURN ? null : inducedReturn
            )));
            return;
        }

        Object res = null;
        state = 0;

        frame.onPush();
        while (state == 0) {
            try {
                res = frame.next(inducedValue, inducedReturn, inducedError);
                inducedValue = inducedReturn = Runners.NO_RETURN;
                inducedError = null;

                if (res != Runners.NO_RETURN) {
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
                    next(ctx, val, Runners.NO_RETURN, null);
                }
                @Override public void onReject(EngineException err) {
                    next(ctx, Runners.NO_RETURN, Runners.NO_RETURN, err);
                }
            });
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
        if (args.has(0)) next(args.ctx, args.get(0), Runners.NO_RETURN, null);
        else next(args.ctx, Runners.NO_RETURN, Runners.NO_RETURN, null);
        return this.currPromise;
    }
    @Expose public PromiseLib __return(Arguments args) {
        this.currPromise = new PromiseLib();
        next(args.ctx, Runners.NO_RETURN, args.get(0), null);
        return this.currPromise;
    }
    @Expose public PromiseLib __throw(Arguments args) {
        this.currPromise = new PromiseLib();
        next(args.ctx, Runners.NO_RETURN, Runners.NO_RETURN, new EngineException(args.get(0)).setCtx(args.ctx));
        return this.currPromise;
    }
}