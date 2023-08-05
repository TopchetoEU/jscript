package me.topchetoeu.jscript.polyfills;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;

public class Promise {
    private static record Handle(CallContext ctx, FunctionValue fulfilled, FunctionValue rejected) {}

    @Native("resolve")
    public static Promise ofResolved(CallContext engine, Object val) {
        if (Values.isWrapper(val, Promise.class)) return Values.wrapper(val, Promise.class);
        var res = new Promise();
        res.fulfill(engine, val);
        return res;
    }
    public static Promise ofResolved(Object val) {
        if (Values.isWrapper(val, Promise.class)) return Values.wrapper(val, Promise.class);
        var res = new Promise();
        res.fulfill(val);
        return res;
    }

    @Native("reject")
    public static Promise ofRejected(CallContext engine, Object val) {
        var res = new Promise();
        res.reject(engine, val);
        return res;
    }
    public static Promise ofRejected(Object val) {
        var res = new Promise();
        res.fulfill(val);
        return res;
    }

    @Native
    public static Promise any(CallContext engine, Object _promises) {
        if (!Values.isArray(_promises)) throw EngineException.ofType("Expected argument for any to be an array.");
        var promises = Values.array(_promises); 
        if (promises.size() == 0) return ofResolved(new ArrayValue());
        var n = new int[] { promises.size() };
        var res = new Promise();

        var errors = new ArrayValue();

        for (var i = 0; i < promises.size(); i++) {
            var index = i;
            var val = promises.get(i);
            if (Values.isWrapper(val, Promise.class)) Values.wrapper(val, Promise.class).then(
                engine, 
                new NativeFunction(null, (e, th, args) -> { res.fulfill(e, args[0]); return null; }),
                new NativeFunction(null, (e, th, args) -> {
                    errors.set(index, args[0]);
                    n[0]--;
                    if (n[0] <= 0) res.reject(e, errors);
                    return null;
                })
            );
            else {
                res.fulfill(engine, val);
                break;
            }
        }

        return res;
    }
    @Native
    public static Promise race(CallContext engine, Object _promises) {
        if (!Values.isArray(_promises)) throw EngineException.ofType("Expected argument for any to be an array.");
        var promises = Values.array(_promises); 
        if (promises.size() == 0) return ofResolved(new ArrayValue());
        var res = new Promise();

        for (var i = 0; i < promises.size(); i++) {
            var val = promises.get(i);
            if (Values.isWrapper(val, Promise.class)) Values.wrapper(val, Promise.class).then(
                engine, 
                new NativeFunction(null, (e, th, args) -> { res.fulfill(e, args[0]); return null; }),
                new NativeFunction(null, (e, th, args) -> { res.reject(e, args[0]); return null; })
            );
            else {
                res.fulfill(val);
                break;
            }
        }

        return res;
    }
    @Native
    public static Promise all(CallContext engine, Object _promises) {
        if (!Values.isArray(_promises)) throw EngineException.ofType("Expected argument for any to be an array.");
        var promises = Values.array(_promises); 
        if (promises.size() == 0) return ofResolved(new ArrayValue());
        var n = new int[] { promises.size() };
        var res = new Promise();

        var result = new ArrayValue();

        for (var i = 0; i < promises.size(); i++) {
            var index = i;
            var val = promises.get(i);
            if (Values.isWrapper(val, Promise.class)) Values.wrapper(val, Promise.class).then(
                engine, 
                new NativeFunction(null, (e, th, args) -> {
                    result.set(index, args[0]);
                    n[0]--;
                    if (n[0] <= 0) res.fulfill(e, result);
                    return null;
                }),
                new NativeFunction(null, (e, th, args) -> { res.reject(e, args[0]); return null; })
            );
            else {
                result.set(i, val);
                break;
            }
        }

        if (n[0] <= 0) res.fulfill(engine, result);

        return res;
    }
    @Native
    public static Promise allSettled(CallContext engine, Object _promises) {
        if (!Values.isArray(_promises)) throw EngineException.ofType("Expected argument for any to be an array.");
        var promises = Values.array(_promises); 
        if (promises.size() == 0) return ofResolved(new ArrayValue());
        var n = new int[] { promises.size() };
        var res = new Promise();

        var result = new ArrayValue();

        for (var i = 0; i < promises.size(); i++) {
            var index = i;
            var val = promises.get(i);
            if (Values.isWrapper(val, Promise.class)) Values.wrapper(val, Promise.class).then(
                engine,
                new NativeFunction(null, (e, th, args) -> {
                    result.set(index, new ObjectValue(Map.of(
                        "status", "fulfilled",
                        "value", args[0]
                    )));
                    n[0]--;
                    if (n[0] <= 0) res.fulfill(e, result);
                    return null;
                }),
                new NativeFunction(null, (e, th, args) -> {
                    result.set(index, new ObjectValue(Map.of(
                        "status", "rejected",
                        "reason", args[0]
                    )));
                    n[0]--;
                    if (n[0] <= 0) res.fulfill(e, result);
                    return null;
                })
            );
            else {
                result.set(i, new ObjectValue(Map.of(
                    "status", "fulfilled",
                    "value", val
                )));
                n[0]--;
            }
        }

        if (n[0] <= 0) res.fulfill(engine, result);

        return res;
    }

    private List<Handle> handles = new ArrayList<>();

    private static final int STATE_PENDING = 0;
    private static final int STATE_FULFILLED = 1;
    private static final int STATE_REJECTED = 2;

    private int state = STATE_PENDING;
    private Object val;

    /**
     * Thread safe - call from any thread
     */
    public void fulfill(Object val) {
        if (Values.isWrapper(val, Promise.class)) throw new IllegalArgumentException("A promise may not be a fulfil value.");
        if (state != STATE_PENDING) return;

        this.state = STATE_FULFILLED;
        this.val = val;
        for (var el : handles) el.ctx().engine().pushMsg(true, el.fulfilled, el.ctx().data(), null, val);
        handles = null;
    }
    /**
     * Thread safe - call from any thread
     */
    public void fulfill(CallContext ctx, Object val) {
        if (Values.isWrapper(val, Promise.class)) Values.wrapper(val, Promise.class).then(ctx,
            new NativeFunction(null, (e, th, args) -> {
                this.fulfill(args[0]);
                return null;
            }),
            new NativeFunction(null, (e, th, args) -> {
                this.reject(args[0]);
                return null;
            })
        );
        else this.fulfill(val);
    }
    /**
     * Thread safe - call from any thread
     */
    public void reject(Object val) {
        if (Values.isWrapper(val, Promise.class)) throw new IllegalArgumentException("A promise may not be a reject value.");
        if (state != STATE_PENDING) return;

        this.state = STATE_REJECTED;
        this.val = val;
        for (var el : handles) el.ctx().engine().pushMsg(true, el.rejected, el.ctx().data(), null, val);
        handles = null;
    }
    /**
     * Thread safe - call from any thread
     */
    public void reject(CallContext ctx, Object val) {
        if (Values.isWrapper(val, Promise.class)) Values.wrapper(val, Promise.class).then(ctx,
            new NativeFunction(null, (e, th, args) -> {
                this.reject(args[0]);
                return null;
            }),
            new NativeFunction(null, (e, th, args) -> {
                this.reject(args[0]);
                return null;
            })
        );
        else this.reject(val);
    }

    private void handle(CallContext ctx, FunctionValue fulfill, FunctionValue reject) {
        if (state == STATE_FULFILLED) ctx.engine().pushMsg(true, fulfill, ctx.data(), null, val);
        else if (state == STATE_REJECTED) ctx.engine().pushMsg(true, reject, ctx.data(), null, val);
        else handles.add(new Handle(ctx, fulfill, reject));
    }

    /**
     * Thread safe - you can call this from anywhere
     * HOWEVER, it's strongly recommended to use this only in javascript
     */
    @Native
    public Promise then(CallContext ctx, Object onFulfill, Object onReject) {
        if (!(onFulfill instanceof FunctionValue)) onFulfill = null;
        if (!(onReject instanceof FunctionValue)) onReject = null;

        var res = new Promise();

        var fulfill = (FunctionValue)onFulfill;
        var reject = (FunctionValue)onReject;

        handle(ctx,
            new NativeFunction(null, (e, th, args) -> {
                if (fulfill == null) res.fulfill(e, args[0]);
                else {
                    try { res.fulfill(e, fulfill.call(e, null, args[0])); }
                    catch (EngineException err) { res.reject(e, err.value); }
                }
                return null;
            }),
            new NativeFunction(null, (e, th, args) -> {
                if (reject == null) res.reject(e, args[0]);
                else {
                    try { res.fulfill(e, reject.call(e, null, args[0])); }
                    catch (EngineException err) { res.reject(e, err.value); }
                }
                return null;
            })
        );

        return res;
    }
    /**
     * Thread safe - you can call this from anywhere
     * HOWEVER, it's strongly recommended to use this only in javascript
     */
    @Native("catch")
    public Promise _catch(CallContext ctx, Object onReject) {
        return then(ctx, null, onReject);
    }
    /**
     * Thread safe - you can call this from anywhere
     * HOWEVER, it's strongly recommended to use this only in javascript
     */
    @Native("finally")
    public Promise _finally(CallContext ctx, Object handle) {
        return then(ctx,
            new NativeFunction(null, (e, th, args) -> {
                if (handle instanceof FunctionValue) ((FunctionValue)handle).call(ctx);
                return args[0];
            }),
            new NativeFunction(null, (e, th, args) -> {
                if (handle instanceof FunctionValue) ((FunctionValue)handle).call(ctx);
                throw new EngineException(args[0]);
            })
        );
    }

    /**
     * NOT THREAD SAFE - must be called from the engine executor thread
     */
    @Native
    public Promise(CallContext ctx, FunctionValue func) throws InterruptedException {
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

    @Override @Native
    public String toString() {
        if (state == STATE_PENDING) return "Promise (pending)";
        else if (state == STATE_FULFILLED) return "Promise (fulfilled)";
        else return "Promise (rejected)";
    }

    private Promise(int state, Object val) {
        this.state = state;
        this.val = val;
    }
    public Promise() {
        this(STATE_PENDING, null);
    }
}
