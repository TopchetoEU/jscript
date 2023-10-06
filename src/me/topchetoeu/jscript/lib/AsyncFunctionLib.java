package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.frame.Runners;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.exceptions.EngineException;

public class AsyncFunctionLib extends FunctionValue {
    public final FunctionValue factory;

    public static class AsyncHelper {
        public PromiseLib promise = new PromiseLib();
        public CodeFrame frame;

        private boolean awaiting = false;

        private void next(Context ctx, Object inducedValue, Object inducedError) throws InterruptedException {
            Object res = null;
            ctx.message.pushFrame(ctx, frame);

            awaiting = false;
            while (!awaiting) {
                try {
                    res = frame.next(ctx, inducedValue, Runners.NO_RETURN, inducedError == Runners.NO_RETURN ? null : new EngineException(inducedError));
                    inducedValue = inducedError = Runners.NO_RETURN;
                    if (res != Runners.NO_RETURN) {
                        promise.fulfill(ctx, res);
                        break;
                    }
                }
                catch (EngineException e) {
                    promise.reject(ctx, e.value);
                    break;
                }
            }

            ctx.message.popFrame(frame);

            if (awaiting) {
                PromiseLib.then(ctx, frame.pop(), new NativeFunction(this::fulfill), new NativeFunction(this::reject));
            }
        }

        public Object fulfill(Context ctx, Object thisArg, Object ...args) throws InterruptedException {
            next(ctx, args.length > 0 ? args[0] : null, Runners.NO_RETURN);
            return null;
        }
        public Object reject(Context ctx, Object thisArg, Object ...args) throws InterruptedException {
            next(ctx, Runners.NO_RETURN, args.length > 0 ? args[0] : null);
            return null;
        }

        public Object await(Context ctx, Object thisArg, Object[] args) {
            this.awaiting = true;
            return args.length > 0 ? args[0] : null;
        }
    }

    @Override
    public Object call(Context ctx, Object thisArg, Object ...args) throws InterruptedException {
        var handler = new AsyncHelper();
        var func = factory.call(ctx, thisArg, new NativeFunction("await", handler::await));
        if (!(func instanceof CodeFunction)) throw EngineException.ofType("Return value of argument must be a js function.");
        handler.frame = new CodeFrame(ctx, thisArg, args, (CodeFunction)func);
        handler.next(ctx, Runners.NO_RETURN, Runners.NO_RETURN);
        return handler.promise;
    }

    public AsyncFunctionLib(FunctionValue factory) {
        super(factory.name, factory.length);
        this.factory = factory;
    }
}
