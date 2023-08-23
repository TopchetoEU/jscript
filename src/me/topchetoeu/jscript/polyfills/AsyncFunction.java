package me.topchetoeu.jscript.polyfills;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.frame.Runners;
import me.topchetoeu.jscript.engine.values.ArrayValue;
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
        private final NativeFunction fulfillFunc = new NativeFunction("", this::fulfill);
        private final NativeFunction rejectFunc = new NativeFunction("", this::reject);

        private Object reject(CallContext ctx, Object thisArg, Object[] args) throws InterruptedException {
            if (args.length > 0) promise.reject(ctx, args[0]);
            return null;
        }
        public Object fulfill(CallContext ctx, Object thisArg, Object[] args) throws InterruptedException {
            if (args.length == 1) frame.push(args[0]);

            while (true) {
                awaiting = false;
                awaited = null;

                try {
                    var res = frame.next(ctx);
                    if (res != Runners.NO_RETURN) {
                        promise.fulfill(ctx, res);
                        return null;
                    }
                }
                catch (EngineException e) {
                    promise.reject(e);
                    return null;
                }

                if (!awaiting) continue;

                frame.pop();

                if (awaited instanceof Promise) ((Promise)awaited).then(ctx, fulfillFunc, rejectFunc);
                else if (Values.isPrimitive(awaited)) frame.push(awaited);
                try {
                    var res = Values.getMember(ctx, awaited, "then");
                    if (res instanceof FunctionValue) {
                        Values.function(res).call(ctx, awaited, fulfillFunc, rejectFunc);
                        return null;
                    }
                    else frame.push(awaited);
                }
                catch (EngineException e) {
                    promise.reject(e);
                    return null;
                }
            }
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
        handler.frame = new CodeFrame(thisArg, new Object[] { new NativeFunction("await", handler::await), new ArrayValue(args) }, body);
        handler.fulfill(_ctx, null, new Object[0]);
        return handler.promise;
    }

    public AsyncFunction(CodeFunction body) {
        super(body.name, body.length);
        this.body = body;
    }
}
