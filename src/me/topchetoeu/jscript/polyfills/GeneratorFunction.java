package me.topchetoeu.jscript.polyfills;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.frame.Runners;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;

public class GeneratorFunction extends FunctionValue {
    public final FunctionValue factory;

    public static class Generator {
        private boolean yielding = true;
        private boolean done = false;
        public CodeFrame frame;

        private ObjectValue next(Context ctx, Object inducedValue, Object inducedReturn, Object inducedError) throws InterruptedException {
            if (done) {
                if (inducedError != Runners.NO_RETURN) throw new EngineException(inducedError);
                var res = new ObjectValue();
                res.defineProperty(ctx, "done", true);
                res.defineProperty(ctx, "value", inducedReturn == Runners.NO_RETURN ? null : inducedReturn);
                return res;
            }

            Object res = null;
            if (inducedValue != Runners.NO_RETURN) frame.push(ctx, inducedValue);
            ctx.message.pushFrame(frame);
            yielding = false;
            while (!yielding) {
                try {
                    res = frame.next(ctx, inducedReturn, inducedError);
                    inducedReturn = inducedError = Runners.NO_RETURN;
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

            ctx.message.popFrame(frame);
            if (done) frame = null;
            else res = frame.pop();

            var obj = new ObjectValue();
            obj.defineProperty(ctx, "done", done);
            obj.defineProperty(ctx, "value", res);
            return obj;
        }

        @Native
        public ObjectValue next(Context ctx, Object ...args) throws InterruptedException {
            if (args.length == 0) return next(ctx, Runners.NO_RETURN, Runners.NO_RETURN, Runners.NO_RETURN);
            else return next(ctx, args[0], Runners.NO_RETURN, Runners.NO_RETURN);
        }
        @Native("throw")
        public ObjectValue _throw(Context ctx, Object error) throws InterruptedException {
            return next(ctx, Runners.NO_RETURN, Runners.NO_RETURN, error);
        }
        @Native("return")
        public ObjectValue _return(Context ctx, Object value) throws InterruptedException {
            return next(ctx, Runners.NO_RETURN, value, Runners.NO_RETURN);
        }

        @Override
        public String toString() {
            if (done) return "Generator [closed]";
            if (yielding) return "Generator [suspended]";
            return "Generator " + (done ? "[closed]" : "[suspended]");
        }

        public Object yield(Context ctx, Object thisArg, Object[] args) {
            this.yielding = true;
            return args.length > 0 ? args[0] : null;
        }
    }

    @Override
    public Object call(Context ctx, Object thisArg, Object ...args) throws InterruptedException {
        var handler = new Generator();
        var func = factory.call(ctx, thisArg, new NativeFunction("yield", handler::yield));
        if (!(func instanceof CodeFunction)) throw EngineException.ofType("Return value of argument must be a js function.");
        handler.frame = new CodeFrame(ctx, thisArg, args, (CodeFunction)func);
        return handler;
    }

    public GeneratorFunction(FunctionValue factory) {
        super(factory.name, factory.length);
        this.factory = factory;
    }
}
