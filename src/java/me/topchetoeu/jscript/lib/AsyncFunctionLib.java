package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.core.Context;
import me.topchetoeu.jscript.core.Frame;
import me.topchetoeu.jscript.core.values.CodeFunction;
import me.topchetoeu.jscript.core.values.FunctionValue;
import me.topchetoeu.jscript.core.values.NativeFunction;
import me.topchetoeu.jscript.core.values.Values;
import me.topchetoeu.jscript.core.exceptions.EngineException;
import me.topchetoeu.jscript.lib.PromiseLib.Handle;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("AsyncFunction")
public class AsyncFunctionLib extends FunctionValue {
    public final FunctionValue factory;

    private static class AsyncHelper {
        public PromiseLib promise = new PromiseLib();
        public Frame frame;

        private boolean awaiting = false;

        private void next(Context ctx, Object inducedValue, EngineException inducedError) {
            Object res = null;

            frame.onPush();
            awaiting = false;
            while (!awaiting) {
                try {
                    res = frame.next(inducedValue, Values.NO_RETURN, inducedError);
                    inducedValue = Values.NO_RETURN;
                    inducedError = null;

                    if (res != Values.NO_RETURN) {
                        promise.fulfill(ctx, res);
                        break;
                    }
                }
                catch (EngineException e) {
                    promise.reject(ctx, e);
                    break;
                }
            }
            frame.onPop();

            if (awaiting) {
                PromiseLib.handle(ctx, frame.pop(), new Handle() {
                    @Override
                    public void onFulfil(Object val) {
                        next(ctx, val, null);
                    }
                    @Override
                    public void onReject(EngineException err) {
                        next(ctx, Values.NO_RETURN, err);
                    }
                });
            }
        }

        public Object await(Arguments args) {
            this.awaiting = true;
            return args.get(0);
        }
    }

    @Override
    public Object call(Context ctx, Object thisArg, Object ...args) {
        var handler = new AsyncHelper();
        var func = factory.call(ctx, thisArg, new NativeFunction("await", handler::await));
        if (!(func instanceof CodeFunction)) throw EngineException.ofType("Return value of argument must be a js function.");
        handler.frame = new Frame(ctx, thisArg, args, (CodeFunction)func);
        handler.next(ctx, Values.NO_RETURN, null);
        return handler.promise;
    }

    public AsyncFunctionLib(FunctionValue factory) {
        super(factory.name, factory.length);
        this.factory = factory;
    }
}
