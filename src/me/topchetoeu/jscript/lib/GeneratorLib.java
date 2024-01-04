package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.frame.Runners;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.Expose;
import me.topchetoeu.jscript.interop.WrapperName;

@WrapperName("Generator")
public class GeneratorLib {
    private boolean yielding = true;
    private boolean done = false;
    public CodeFrame frame;

    private ObjectValue next(Context ctx, Object inducedValue, Object inducedReturn, EngineException inducedError) {
        if (done) {
            if (inducedError != Runners.NO_RETURN) throw inducedError;
            var res = new ObjectValue();
            res.defineProperty(ctx, "done", true);
            res.defineProperty(ctx, "value", inducedReturn == Runners.NO_RETURN ? null : inducedReturn);
            return res;
        }

        Object res = null;
        yielding = false;

        frame.onPush();
        while (!yielding) {
            try {
                res = frame.next(inducedValue, inducedReturn, inducedError);
                inducedReturn = Runners.NO_RETURN;
                inducedError = null;
                if (res != Runners.NO_RETURN) {
                    done = true;
                    break;
                }
            }
            catch (EngineException e) {
                done = true;
                throw e;
            }
        }
        frame.onPop();

        if (done) frame = null;
        else res = frame.pop();

        var obj = new ObjectValue();
        obj.defineProperty(ctx, "done", done);
        obj.defineProperty(ctx, "value", res);
        return obj;
    }

    @Expose public ObjectValue __next(Arguments args) {
        if (args.n() == 0) return next(args.ctx, Runners.NO_RETURN, Runners.NO_RETURN, null);
        else return next(args.ctx, args.get(0), Runners.NO_RETURN, null);
    }
    @Expose public ObjectValue __throw(Arguments args) {
        return next(args.ctx, Runners.NO_RETURN, Runners.NO_RETURN, new EngineException(args.get(0)).setCtx(args.ctx));
    }
    @Expose public ObjectValue __return(Arguments args) {
        return next(args.ctx, Runners.NO_RETURN, args.get(0), null);
    }

    @Override public String toString() {
        if (done) return "Generator [closed]";
        if (yielding) return "Generator [suspended]";
        return "Generator [running]";
    }

    public Object yield(Arguments args) {
        this.yielding = true;
        return args.get(0);
    }
}