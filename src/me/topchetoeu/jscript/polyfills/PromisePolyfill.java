package me.topchetoeu.jscript.polyfills;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.MessageContext;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.NativeWrapper;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;

public class PromisePolyfill {
    private static class Handle {
        public final Context ctx;
        public final FunctionValue fulfilled;
        public final FunctionValue rejected;

        public Handle(Context ctx, FunctionValue fulfilled, FunctionValue rejected) {
            this.ctx = ctx;
            this.fulfilled = fulfilled;
            this.rejected = rejected;
        }
    }

    @Native("resolve")
    public static PromisePolyfill ofResolved(Context ctx, Object val) throws InterruptedException {
        var res = new PromisePolyfill();
        res.fulfill(ctx, val);
        return res;
    }
    @Native("reject")
    public static PromisePolyfill ofRejected(Context ctx, Object val) throws InterruptedException {
        var res = new PromisePolyfill();
        res.reject(ctx, val);
        return res;
    }

    @Native public static PromisePolyfill any(Context ctx, Object _promises) throws InterruptedException {
        if (!Values.isArray(_promises)) throw EngineException.ofType("Expected argument for any to be an array.");
        var promises = Values.array(_promises); 
        if (promises.size() == 0) return ofResolved(ctx, new ArrayValue());
        var n = new int[] { promises.size() };
        var res = new PromisePolyfill();

        var errors = new ArrayValue();

        for (var i = 0; i < promises.size(); i++) {
            var index = i;
            var val = promises.get(i);
            then(ctx, val,
                new NativeFunction(null, (e, th, args) -> { res.fulfill(e, args[0]); return null; }),
                new NativeFunction(null, (e, th, args) -> {
                    errors.set(ctx, index, args[0]);
                    n[0]--;
                    if (n[0] <= 0) res.reject(e, errors);
                    return null;
                })
            );
        }

        return res;
    }
    @Native public static PromisePolyfill race(Context ctx, Object _promises) throws InterruptedException  {
        if (!Values.isArray(_promises)) throw EngineException.ofType("Expected argument for any to be an array.");
        var promises = Values.array(_promises); 
        if (promises.size() == 0) return ofResolved(ctx, new ArrayValue());
        var res = new PromisePolyfill();

        for (var i = 0; i < promises.size(); i++) {
            var val = promises.get(i);
            then(ctx, val,
                new NativeFunction(null, (e, th, args) -> { res.fulfill(e, args[0]); return null; }),
                new NativeFunction(null, (e, th, args) -> { res.reject(e, args[0]); return null; })
            );
        }

        return res;
    }
    @Native public static PromisePolyfill all(Context ctx, Object _promises) throws InterruptedException  {
        if (!Values.isArray(_promises)) throw EngineException.ofType("Expected argument for any to be an array.");
        var promises = Values.array(_promises); 
        if (promises.size() == 0) return ofResolved(ctx, new ArrayValue());
        var n = new int[] { promises.size() };
        var res = new PromisePolyfill();

        var result = new ArrayValue();

        for (var i = 0; i < promises.size(); i++) {
            var index = i;
            var val = promises.get(i);
            then(ctx, val,
                new NativeFunction(null, (e, th, args) -> {
                    result.set(ctx, index, args[0]);
                    n[0]--;
                    if (n[0] <= 0) res.fulfill(e, result);
                    return null;
                }),
                new NativeFunction(null, (e, th, args) -> { res.reject(e, args[0]); return null; })
            );
        }

        if (n[0] <= 0) res.fulfill(ctx, result);

        return res;
    }
    @Native public static PromisePolyfill allSettled(Context ctx, Object _promises) throws InterruptedException  {
        if (!Values.isArray(_promises)) throw EngineException.ofType("Expected argument for any to be an array.");
        var promises = Values.array(_promises); 
        if (promises.size() == 0) return ofResolved(ctx, new ArrayValue());
        var n = new int[] { promises.size() };
        var res = new PromisePolyfill();

        var result = new ArrayValue();

        for (var i = 0; i < promises.size(); i++) {
            var index = i;
            var val = promises.get(i);
            then(ctx, val,
                new NativeFunction(null, (e, th, args) -> {
                    result.set(ctx, index, new ObjectValue(ctx, Map.of(
                        "status", "fulfilled",
                        "value", args[0]
                    )));
                    n[0]--;
                    if (n[0] <= 0) res.fulfill(e, result);
                    return null;
                }),
                new NativeFunction(null, (e, th, args) -> {
                    result.set(ctx, index, new ObjectValue(ctx, Map.of(
                        "status", "rejected",
                        "reason", args[0]
                    )));
                    n[0]--;
                    if (n[0] <= 0) res.fulfill(e, result);
                    return null;
                })
            );
        }

        if (n[0] <= 0) res.fulfill(ctx, result);

        return res;
    }

    /**
     * Thread safe - you can call this from anywhere
     * HOWEVER, it's strongly recommended to use this only in javascript
     */
    @Native(thisArg=true) public static Object then(Context ctx, Object thisArg, Object _onFulfill, Object _onReject) throws InterruptedException {
        var onFulfill = _onFulfill instanceof FunctionValue ? ((FunctionValue)_onFulfill) : null;
        var onReject = _onReject instanceof FunctionValue ? ((FunctionValue)_onReject) : null;

        var res = new PromisePolyfill();

        var fulfill = onFulfill == null ? new NativeFunction((_ctx, _thisArg, _args) -> _args.length > 0 ? _args[0] : null) : (FunctionValue)onFulfill;
        var reject = onReject == null ? new NativeFunction((_ctx, _thisArg, _args) -> {
            throw new EngineException(_args.length > 0 ? _args[0] : null);
        }) : (FunctionValue)onReject;

        if (thisArg instanceof NativeWrapper && ((NativeWrapper)thisArg).wrapped instanceof PromisePolyfill) {
            thisArg = ((NativeWrapper)thisArg).wrapped;
        }

        var fulfillHandle = new NativeFunction(null, (_ctx, th, a) -> {
            try {
                res.fulfill(ctx, Values.convert(ctx, fulfill.call(ctx, null, a[0]), Object.class));
            }
            catch (EngineException err) { res.reject(ctx, err.value); }
            return null;
        });
        var rejectHandle = new NativeFunction(null, (_ctx, th, a) -> {
            try { res.fulfill(ctx, reject.call(ctx, null, a[0])); }
            catch (EngineException err) { res.reject(ctx, err.value); }
            return null;
        });

        if (thisArg instanceof PromisePolyfill) ((PromisePolyfill)thisArg).handle(ctx, fulfillHandle, rejectHandle);
        else {
            Object next;
            try {
                next = Values.getMember(ctx, thisArg, "then");
            }
            catch (IllegalArgumentException e) { next = null; }

            try {
                if (next instanceof FunctionValue) ((FunctionValue)next).call(ctx, thisArg, fulfillHandle, rejectHandle);
                else res.fulfill(ctx, fulfill.call(ctx, null, thisArg));
            }
            catch (EngineException err) {
                res.reject(ctx, fulfill.call(ctx, null, err.value));
            }
        }

        return res;
    }
    /**
     * Thread safe - you can call this from anywhere
     * HOWEVER, it's strongly recommended to use this only in javascript
     */
    @Native(value="catch", thisArg=true) public static Object _catch(Context ctx, Object thisArg, Object _onReject) throws InterruptedException {
        return then(ctx, thisArg, null, _onReject);
    }
    /**
     * Thread safe - you can call this from anywhere
     * HOWEVER, it's strongly recommended to use this only in javascript
     */
    @Native(value="finally", thisArg=true) public static Object _finally(Context ctx, Object thisArg, Object _handle) throws InterruptedException {
        return then(ctx, thisArg,
            new NativeFunction(null, (e, th, _args) -> {
                if (_handle instanceof FunctionValue) ((FunctionValue)_handle).call(ctx);
                return _args.length > 0 ? _args[0] : null;
            }),
            new NativeFunction(null, (e, th, _args) -> {
                if (_handle instanceof FunctionValue) ((FunctionValue)_handle).call(ctx);
                throw new EngineException(_args.length > 0 ? _args[0] : null);
            })
        );
    }

    private List<Handle> handles = new ArrayList<>();

    private static final int STATE_PENDING = 0;
    private static final int STATE_FULFILLED = 1;
    private static final int STATE_REJECTED = 2;

    private int state = STATE_PENDING;
    private boolean handled = false;
    private Object val;

    private void resolve(Context ctx, Object val, int state) throws InterruptedException {
        if (this.state != STATE_PENDING) return;

        if (val instanceof PromisePolyfill) ((PromisePolyfill)val).handle(ctx,
            new NativeFunction(null, (e, th, a) -> { this.resolve(ctx, a[0], state); return null; }),
            new NativeFunction(null, (e, th, a) -> { this.resolve(ctx, a[0], STATE_REJECTED); return null; })
        );
        else {
            Object next;
            try { next = Values.getMember(ctx, val, "next"); }
            catch (IllegalArgumentException e) { next = null; }

            try {
                if (next instanceof FunctionValue) ((FunctionValue)next).call(ctx, val,
                    new NativeFunction((e, _thisArg, a) -> { this.resolve(ctx, a.length > 0 ? a[0] : null, state); return null; }),
                    new NativeFunction((e, _thisArg, a) -> { this.resolve(ctx, a.length > 0 ? a[0] : null, STATE_REJECTED); return null; })
                );
                else {
                    this.val = val;
                    this.state = state;

                    if (state == STATE_FULFILLED) {
                        for (var handle : handles) handle.fulfilled.call(handle.ctx, null, val);
                    }
                    else if (state == STATE_REJECTED) {
                        for (var handle : handles) handle.rejected.call(handle.ctx, null, val);
                        if (handles.size() == 0) {
                            ctx.message.engine.pushMsg(true, ctx.message, new NativeFunction((_ctx, _thisArg, _args) -> {
                                if (!handled) {
                                    try { Values.printError(new EngineException(val).setContext(ctx), "(in promise)"); }
                                    catch (InterruptedException ex) { }
                                }

                                return null;
                            }), null);
                        }
                    }

                    handles = null;
                }
            }
            catch (EngineException err) {
                this.reject(ctx, err.value);
            }
        }
    }

    /**
     * Thread safe - call from any thread
     */
    public void fulfill(Context ctx, Object val) throws InterruptedException {
        resolve(ctx, val, STATE_FULFILLED);
    }
    /**
     * Thread safe - call from any thread
     */
    public void reject(Context ctx, Object val) throws InterruptedException {
        resolve(ctx, val, STATE_REJECTED);
    }

    private void handle(Context ctx, FunctionValue fulfill, FunctionValue reject) {
        if (state == STATE_FULFILLED) ctx.message.engine.pushMsg(true, new MessageContext(ctx.message.engine), fulfill, null, val);
        else if (state == STATE_REJECTED) {
            ctx.message.engine.pushMsg(true, new MessageContext(ctx.message.engine), reject, null, val);
            handled = true;
        }
        else handles.add(new Handle(ctx, fulfill, reject));
    }

    @Override @Native public String toString() {
        if (state == STATE_PENDING) return "Promise (pending)";
        else if (state == STATE_FULFILLED) return "Promise (fulfilled)";
        else return "Promise (rejected)";
    }

    /**
     * NOT THREAD SAFE - must be called from the engine executor thread
     */
    @Native public PromisePolyfill(Context ctx, FunctionValue func) throws InterruptedException {
        if (!(func instanceof FunctionValue)) throw EngineException.ofType("A function must be passed to the promise constructor.");
        try {
            func.call(
                ctx, null,
                new NativeFunction(null, (e, th, args) -> {
                    fulfill(e, args.length > 0 ? args[0] : null);
                    return null;
                }),
                new NativeFunction(null, (e, th, args) -> {
                    reject(e, args.length > 0 ? args[0] : null);
                    return null;
                })
            );
        }
        catch (EngineException e) {
            reject(ctx, e.value);
        }
    }

    private PromisePolyfill(int state, Object val) {
        this.state = state;
        this.val = val;
    }
    public PromisePolyfill() {
        this(STATE_PENDING, null);
    }
}
