package me.topchetoeu.jscript.polyfills;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.frame.Runners;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;

public class AsyncFunction extends FunctionValue {
    public final CodeFunction body;

    private class CallHandler {
        private boolean awaiting = false;
        private Object awaited = null;
        public final Promise promise = new Promise();
        public CodeFrame frame;
        private final NativeFunction fulfillFunc = new NativeFunction("", (ctx, thisArg, args) -> {
            if (args.length == 0) exec(ctx, null, Runners.NO_RETURN);
            else exec(ctx, args[0], Runners.NO_RETURN);

            return null;
        });
        private final NativeFunction rejectFunc = new NativeFunction("", (ctx, thisArg, args) -> {
            if (args.length == 0) exec(ctx, Runners.NO_RETURN, null);
            else exec(ctx, Runners.NO_RETURN, args[0]);

            return null;
        });

        public Object exec(CallContext ctx, Object val, Object err) throws InterruptedException {
            if (val != Runners.NO_RETURN) frame.push(val);

            frame.start(ctx);

            while (true) {
                awaiting = false;
                awaited = null;

                try {
                    var res = frame.next(ctx, val, err);
                    err = Runners.NO_RETURN;
                    if (res != Runners.NO_RETURN) {
                        promise.fulfill(ctx, res);
                        break;
                    }
                }
                catch (EngineException e) {
                    promise.reject(e.value);
                    break;
                }

                if (!awaiting) continue;

                frame.pop();

                if (awaited instanceof Promise) ((Promise)awaited).then(ctx, fulfillFunc, rejectFunc);
                else if (Values.isPrimitive(awaited)) frame.push(awaited);
                else {
                    try {
                        var res = Values.getMember(ctx, awaited, "then");
                        if (res instanceof FunctionValue) {
                            Values.function(res).call(ctx, awaited, fulfillFunc, rejectFunc);
                            break;
                        }
                        else frame.push(awaited);
                    }
                    catch (EngineException e) {
                        promise.reject(e);
                        break;
                    }
                }
            }

            frame.end(ctx);
            return null;
        }

        public Object await(CallContext ctx, Object thisArg, Object[] args) {
            this.awaiting = true;
            this.awaited = args[0];
            return null;
        }
    }

    @Override
    public Object call(CallContext _ctx, Object thisArg, Object... args) throws InterruptedException {
        var handler = new CallHandler();
        var func = body.call(_ctx, thisArg, new NativeFunction("await", handler::await));
        if (!(func instanceof CodeFunction)) throw EngineException.ofType("Return value of argument must be a js function.");
        handler.frame = new CodeFrame(thisArg, args, (CodeFunction)func);
        handler.exec(_ctx, Runners.NO_RETURN, Runners.NO_RETURN);
        return handler.promise;
    }

    public AsyncFunction(CodeFunction body) {
        super(body.name, body.length);
        this.body = body;
    }
}
