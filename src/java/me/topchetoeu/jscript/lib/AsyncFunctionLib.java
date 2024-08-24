package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.lib.PromiseLib.Handle;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.CodeFunction;
import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.NativeFunction;
import me.topchetoeu.jscript.runtime.values.Values;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("AsyncFunction")
public class AsyncFunctionLib extends FunctionValue {
    public final CodeFunction func;

    private static class AsyncHelper {
        public PromiseLib promise = new PromiseLib();
        public Frame frame;

        private boolean awaiting = false;

        private void next(Environment env, Object inducedValue, EngineException inducedError) {
            Object res = null;

            frame.onPush();
            awaiting = false;
            while (!awaiting) {
                try {
                    if (inducedValue != Values.NO_RETURN) res = frame.next(inducedValue);
                    else if (inducedError != null) res = frame.induceError(inducedError);
                    else res = frame.next();

                    inducedValue = Values.NO_RETURN;
                    inducedError = null;

                    if (res != Values.NO_RETURN) {
                        promise.fulfill(env, res);
                        break;
                    }
                }
                catch (EngineException e) {
                    promise.reject(env, e);
                    break;
                }
            }
            frame.onPop();

            if (awaiting) {
                PromiseLib.handle(env, frame.pop(), new Handle() {
                    @Override
                    public void onFulfil(Object val) {
                        next(env, val, null);
                    }
                    @Override
                    public void onReject(EngineException err) {
                        next(env, Values.NO_RETURN, err);
                    }
                }.defer(env));
            }
        }

        public Object await(Arguments args) {
            this.awaiting = true;
            return args.get(0);
        }
    }

    @Override
    public Object call(Environment env, Object thisArg, Object ...args) {
        var handler = new AsyncHelper();

        var newArgs = new Object[args.length + 1];
        newArgs[0] = new NativeFunction("await", handler::await);
        System.arraycopy(args, 0, newArgs, 1, args.length);

        handler.frame = new Frame(env, thisArg, newArgs, (CodeFunction)func);
        handler.next(env, Values.NO_RETURN, null);
        return handler.promise;
    }

    public AsyncFunctionLib(FunctionValue func) {
        super(func.name, func.length);
        if (!(func instanceof CodeFunction)) throw EngineException.ofType("Return value of argument must be a js function.");
        this.func = (CodeFunction)func;
    }
}
