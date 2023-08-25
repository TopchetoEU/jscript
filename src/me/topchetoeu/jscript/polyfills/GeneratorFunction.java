package me.topchetoeu.jscript.polyfills;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.frame.Runners;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;

public class GeneratorFunction extends FunctionValue {
    public final CodeFunction factory;

    public static class Generator {
        private boolean yielding = true;
        private boolean done = false;
        public CodeFrame frame;

        private ObjectValue next(CallContext ctx, Object inducedValue, Object inducedReturn, Object inducedError) throws InterruptedException {
            if (done) {
                if (inducedError != Runners.NO_RETURN) throw new EngineException(inducedError);
                var res = new ObjectValue();
                res.defineProperty("done", true);
                res.defineProperty("value", inducedReturn == Runners.NO_RETURN ? null : inducedReturn);
                return res;
            }

            Object res = null;
            if (inducedValue != Runners.NO_RETURN) frame.push(inducedValue);
            frame.start(ctx);
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

            frame.end(ctx);
            if (done) frame = null;
            else res = frame.pop();

            var obj = new ObjectValue();
            obj.defineProperty("done", done);
            obj.defineProperty("value", res);
            return obj;
        }

        @Native
        public ObjectValue next(CallContext ctx, Object... args) throws InterruptedException {
            if (args.length == 0) return next(ctx, Runners.NO_RETURN, Runners.NO_RETURN, Runners.NO_RETURN);
            else return next(ctx, args[0], Runners.NO_RETURN, Runners.NO_RETURN);
        }
        @Native("throw")
        public ObjectValue _throw(CallContext ctx, Object error) throws InterruptedException {
            return next(ctx, Runners.NO_RETURN, Runners.NO_RETURN, error);
        }
        @Native("return")
        public ObjectValue _return(CallContext ctx, Object value) throws InterruptedException {
            return next(ctx, Runners.NO_RETURN, value, Runners.NO_RETURN);
        }

        @Override
        public String toString() {
            if (done) return "Generator [closed]";
            if (yielding) return "Generator [suspended]";
            return "Generator " + (done ? "[closed]" : "[suspended]");
        }

        public Object yield(CallContext ctx, Object thisArg, Object[] args) {
            this.yielding = true;
            return args.length > 0 ? args[0] : null;
        }
    }

    @Override
    public Object call(CallContext _ctx, Object thisArg, Object... args) throws InterruptedException {
        var handler = new Generator();
        var func = factory.call(_ctx, thisArg, new NativeFunction("yield", handler::yield));
        if (!(func instanceof CodeFunction)) throw EngineException.ofType("Return value of argument must be a js function.");
        handler.frame = new CodeFrame(thisArg, args, (CodeFunction)func);
        return handler;
    }

    public GeneratorFunction(CodeFunction factory) {
        super(factory.name, factory.length);
        this.factory = factory;
    }
}
