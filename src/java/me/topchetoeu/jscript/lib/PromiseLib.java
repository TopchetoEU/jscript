package me.topchetoeu.jscript.lib;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.common.ResultRunnable;
import me.topchetoeu.jscript.core.engine.Context;
import me.topchetoeu.jscript.core.engine.EventLoop;
import me.topchetoeu.jscript.core.engine.values.ArrayValue;
import me.topchetoeu.jscript.core.engine.values.FunctionValue;
import me.topchetoeu.jscript.core.engine.values.NativeFunction;
import me.topchetoeu.jscript.core.engine.values.ObjectValue;
import me.topchetoeu.jscript.core.engine.values.Values;
import me.topchetoeu.jscript.core.exceptions.EngineException;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeConstructor;
import me.topchetoeu.jscript.utils.interop.ExposeTarget;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("Promise")
public class PromiseLib {
    public static interface Handle {
        void onFulfil(Object val);
        void onReject(EngineException err);

        default Handle defer(EventLoop loop) {
            var self = this;
            return new Handle() {
                @Override public void onFulfil(Object val) {
                    loop.pushMsg(() -> self.onFulfil(val), true);
                }
                @Override public void onReject(EngineException val) {
                    loop.pushMsg(() -> self.onReject(val), true);
                }
            };
        }
    }

    private static final int STATE_PENDING = 0;
    private static final int STATE_FULFILLED = 1;
    private static final int STATE_REJECTED = 2;

    private List<Handle> handles = new ArrayList<>();

    private int state = STATE_PENDING;
    private boolean handled = false;
    private Object val;

    private void resolveSynchronized(Context ctx, Object val, int newState) {
        ctx.engine.pushMsg(() -> {
            this.val = val;
            this.state = newState;

            for (var handle : handles) {
                if (newState == STATE_FULFILLED) handle.onFulfil(val);
                if (newState == STATE_REJECTED) {
                    handle.onReject((EngineException)val);
                    handled = true;
                }
            }

            if (state == STATE_REJECTED && !handled) {
                Values.printError(((EngineException)val).setCtx(ctx.environment, ctx.engine), "(in promise)");
            }

            handles = null;
        }, true);
        
    }
    private synchronized void resolve(Context ctx, Object val, int newState) {
        if (this.state != STATE_PENDING || newState == STATE_PENDING) return;

        handle(ctx, val, new Handle() {
            @Override public void onFulfil(Object val) {
                resolveSynchronized(ctx, val, newState);
            }
            @Override public void onReject(EngineException err) {
                resolveSynchronized(ctx, val, STATE_REJECTED);
            }
        });
    }

    public synchronized void fulfill(Context ctx, Object val) {
        resolve(ctx, val, STATE_FULFILLED);
    }
    public synchronized void reject(Context ctx, EngineException val) {
        resolve(ctx, val, STATE_REJECTED);
    }

    private void handle(Handle handle) {
        if (state == STATE_FULFILLED) handle.onFulfil(val);
        else if (state == STATE_REJECTED) {
            handle.onReject((EngineException)val);
            handled = true;
        }
        else handles.add(handle);
    }

    @Override public String toString() {
        if (state == STATE_PENDING) return "Promise (pending)";
        else if (state == STATE_FULFILLED) return "Promise (fulfilled)";
        else return "Promise (rejected)";
    }

    public PromiseLib() {
        this.state = STATE_PENDING;
        this.val = null;
    }

    public static PromiseLib await(Context ctx, ResultRunnable<Object> runner) {
        var res = new PromiseLib();

        new Thread(() -> {
            try {
                res.fulfill(ctx, runner.run());
            }
            catch (EngineException e) {
                res.reject(ctx, e);
            }
        }, "Promisifier").start();

        return res;
    }
    public static PromiseLib await(Context ctx, Runnable runner) {
        return await(ctx, () -> {
            runner.run();
            return null;
        });
    }

    public static void handle(Context ctx, Object obj, Handle handle) {
        if (Values.isWrapper(obj, PromiseLib.class)) {
            var promise = Values.wrapper(obj, PromiseLib.class);
            handle(ctx, promise, handle);
            return;
        }
        if (obj instanceof PromiseLib) {
            ((PromiseLib)obj).handle(handle);
            return;
        }

        var rethrow = new boolean[1];

        try {
            var then = Values.getMember(ctx, obj, "then");
            Values.call(ctx, then, obj,
                new NativeFunction(args -> {
                    try { handle.onFulfil(args.get(0)); }
                    catch (Exception e) {
                        rethrow[0] = true;
                        throw e;
                    }
                    return null;
                }),
                new NativeFunction(args -> {
                    try { handle.onReject(new EngineException(args.get(0))); }
                    catch (Exception e) {
                        rethrow[0] = true;
                        throw e;
                    }
                    return null;
                })
            );

            return;
        }
        catch (Exception e) {
            if (rethrow[0]) throw e;
        }

        handle.onFulfil(obj);
    }

    public static PromiseLib ofResolved(Context ctx, Object value) {
        var res = new PromiseLib();
        res.fulfill(ctx, value);
        return res;
    }
    public static PromiseLib ofRejected(Context ctx, EngineException value) {
        var res = new PromiseLib();
        res.reject(ctx, value);
        return res;
    }

    @Expose(value = "resolve", target = ExposeTarget.STATIC)
    public static PromiseLib __ofResolved(Arguments args) {
        return ofResolved(args.ctx, args.get(0));
    }
    @Expose(value = "reject", target = ExposeTarget.STATIC)
    public static PromiseLib __ofRejected(Arguments args) {
        return ofRejected(args.ctx, new EngineException(args.get(0)).setCtx(args.ctx));
    }

    @Expose(target = ExposeTarget.STATIC)
    public static PromiseLib __any(Arguments args) {
        if (!(args.get(0) instanceof ArrayValue)) throw EngineException.ofType("Expected argument for any to be an array.");
        var promises = args.convert(0, ArrayValue.class); 

        if (promises.size() == 0) return ofRejected(args.ctx, EngineException.ofError("No promises passed to 'Promise.any'.").setCtx(args.ctx));
        var n = new int[] { promises.size() };
        var res = new PromiseLib();
        var errors = new ArrayValue();

        for (var i = 0; i < promises.size(); i++) {
            var index = i;
            var val = promises.get(i);
            if (res.state != STATE_PENDING) break;

            handle(args.ctx, val, new Handle() {
                public void onFulfil(Object val) { res.fulfill(args.ctx, val); }
                public void onReject(EngineException err) {
                    errors.set(args.ctx, index, err.value);
                    n[0]--;
                    if (n[0] <= 0) res.reject(args.ctx, new EngineException(errors).setCtx(args.ctx));
                }
            });
        }

        return res;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static PromiseLib __race(Arguments args) {
        if (!(args.get(0) instanceof ArrayValue)) throw EngineException.ofType("Expected argument for any to be an array.");
        var promises = args.convert(0, ArrayValue.class);
        var res = new PromiseLib();

        for (var i = 0; i < promises.size(); i++) {
            var val = promises.get(i);
            if (res.state != STATE_PENDING) break;

            handle(args.ctx, val, new Handle() {
                @Override public void onFulfil(Object val) { res.fulfill(args.ctx, val); }
                @Override public void onReject(EngineException err) { res.reject(args.ctx, err); }
            });
        }

        return res;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static PromiseLib __all(Arguments args) {
        if (!(args.get(0) instanceof ArrayValue)) throw EngineException.ofType("Expected argument for any to be an array.");
        var promises = args.convert(0, ArrayValue.class); 
        var n = new int[] { promises.size() };
        var res = new PromiseLib();
        var result = new ArrayValue();

        for (var i = 0; i < promises.size(); i++) {
            if (res.state != STATE_PENDING) break;

            var index = i;
            var val = promises.get(i);

            handle(args.ctx, val, new Handle() {
                @Override public void onFulfil(Object val) {
                    result.set(args.ctx, index, val);
                    n[0]--;
                    if (n[0] <= 0) res.fulfill(args.ctx, result);
                }
                @Override public void onReject(EngineException err) {
                    res.reject(args.ctx, err);
                }
            });
        }

        if (n[0] <= 0) res.fulfill(args.ctx, result);

        return res;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static PromiseLib __allSettled(Arguments args) {
        if (!(args.get(0) instanceof ArrayValue)) throw EngineException.ofType("Expected argument for any to be an array.");
        var promises = args.convert(0, ArrayValue.class); 
        var n = new int[] { promises.size() };
        var res = new PromiseLib();
        var result = new ArrayValue();

        for (var i = 0; i < promises.size(); i++) {
            if (res.state != STATE_PENDING) break;

            var index = i;

            handle(args.ctx, promises.get(i), new Handle() {
                @Override public void onFulfil(Object val) {
                    var desc = new ObjectValue();
                    desc.defineProperty(args.ctx, "status", "fulfilled");
                    desc.defineProperty(args.ctx, "value", val);

                    result.set(args.ctx, index, desc);

                    n[0]--;
                    if (n[0] <= 0) res.fulfill(args.ctx, res);
                }
                @Override public void onReject(EngineException err) {
                    var desc = new ObjectValue();
                    desc.defineProperty(args.ctx, "status", "reject");
                    desc.defineProperty(args.ctx, "value", err.value);

                    result.set(args.ctx, index, desc);

                    n[0]--;
                    if (n[0] <= 0) res.fulfill(args.ctx, res);
                }
            });
        }

        if (n[0] <= 0) res.fulfill(args.ctx, result);

        return res;
    }

    @Expose
    public static Object __then(Arguments args) {
        var onFulfill = args.get(0) instanceof FunctionValue ? args.convert(0, FunctionValue.class) : null;
        var onReject = args.get(1) instanceof FunctionValue ? args.convert(1, FunctionValue.class) : null;

        var res = new PromiseLib();

        handle(args.ctx, args.self, new Handle() {
            @Override public void onFulfil(Object val) {
                try { res.fulfill(args.ctx, onFulfill.call(args.ctx, null, val)); }
                catch (EngineException e) { res.reject(args.ctx, e); }
            }
            @Override public void onReject(EngineException err) {
                try { res.fulfill(args.ctx, onReject.call(args.ctx, null, err.value)); }
                catch (EngineException e) { res.reject(args.ctx, e); }
            }
        }.defer(args.ctx.engine));

        return res;
    }
    @Expose
    public static Object __catch(Arguments args) {
        return __then(new Arguments(args.ctx, args.self, null, args.get(0)));
    }
    @Expose
    public static Object __finally(Arguments args) {
        var func = args.get(0) instanceof FunctionValue ? args.convert(0, FunctionValue.class) : null;

        var res = new PromiseLib();

        handle(args.ctx, args.self, new Handle() {
            @Override public void onFulfil(Object val) {
                try {
                    func.call(args.ctx);
                    res.fulfill(args.ctx, val);
                }
                catch (EngineException e) { res.reject(args.ctx, e); }
            }
            @Override public void onReject(EngineException err) {
                try {
                    func.call(args.ctx);
                    res.reject(args.ctx, err);
                }
                catch (EngineException e) { res.reject(args.ctx, e); }
            }
        }.defer(args.ctx.engine));

        return res;
    }

    @ExposeConstructor
    public static PromiseLib __constructor(Arguments args) {
        var func = args.convert(0, FunctionValue.class);
        var res = new PromiseLib();

        try {
            func.call(
                args.ctx, null,
                new NativeFunction(null, _args -> {
                    res.fulfill(_args.ctx, _args.get(0));
                    return null;
                }),
                new NativeFunction(null, _args -> {
                    res.reject(_args.ctx, new EngineException(_args.get(0)).setCtx(_args.ctx));
                    return null;
                })
            );
        }
        catch (EngineException e) {
            res.reject(args.ctx, e);
        }

        return res;
    }
}
