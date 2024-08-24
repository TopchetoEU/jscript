package me.topchetoeu.jscript.lib;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.common.ResultRunnable;
import me.topchetoeu.jscript.runtime.EventLoop;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.exceptions.InterruptException;
import me.topchetoeu.jscript.runtime.values.ArrayValue;
import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.NativeFunction;
import me.topchetoeu.jscript.runtime.values.ObjectValue;
import me.topchetoeu.jscript.runtime.values.Values;
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

        default Handle defer(Environment loop) {
            var self = this;

            return new Handle() {
                @Override public void onFulfil(Object val) {
                    if (!loop.hasNotNull(EventLoop.KEY)) throw EngineException.ofError("No event loop");
                    loop.get(EventLoop.KEY).pushMsg(() -> self.onFulfil(val), true);
                }
                @Override public void onReject(EngineException val) {
                    if (!loop.hasNotNull(EventLoop.KEY)) throw EngineException.ofError("No event loop");
                    loop.get(EventLoop.KEY).pushMsg(() -> self.onReject(val), true);
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

    private void resolveSynchronized(Environment env, Object val, int newState) {
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
            Values.printError(((EngineException)val).setEnvironment(env), "(in promise)");
        }

        handles = null;

        // ctx.get(EventLoop.KEY).pushMsg(() -> {
        //     if (!ctx.hasNotNull(EventLoop.KEY)) throw EngineException.ofError("No event loop");


        //     handles = null;
        // }, true);
        
    }
    private synchronized void resolve(Environment env, Object val, int newState) {
        if (this.state != STATE_PENDING || newState == STATE_PENDING) return;

        handle(env, val, new Handle() {
            @Override public void onFulfil(Object val) {
                resolveSynchronized(env, val, newState);
            }
            @Override public void onReject(EngineException err) {
                resolveSynchronized(env, val, STATE_REJECTED);
            }
        });
    }

    public synchronized void fulfill(Environment env, Object val) {
        resolve(env, val, STATE_FULFILLED);
    }
    public synchronized void reject(Environment env, EngineException val) {
        resolve(env, val, STATE_REJECTED);
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

    public static PromiseLib await(Environment env, ResultRunnable<Object> runner) {
        var res = new PromiseLib();

        new Thread(() -> {
            try {
                res.fulfill(env, runner.run());
            }
            catch (EngineException e) {
                res.reject(env, e);
            }
            catch (Exception e) {
                if (e instanceof InterruptException) throw e;
                else {
                    res.reject(env, EngineException.ofError("Native code failed with " + e.getMessage()));
                }
            }
        }, "Promisifier").start();

        return res;
    }
    public static PromiseLib await(Environment env, Runnable runner) {
        return await(env, () -> {
            runner.run();
            return null;
        });
    }

    public static void handle(Environment env, Object obj, Handle handle) {
        if (Values.isWrapper(obj, PromiseLib.class)) {
            var promise = Values.wrapper(obj, PromiseLib.class);
            handle(env, promise, handle);
            return;
        }
        if (obj instanceof PromiseLib) {
            ((PromiseLib)obj).handle(handle);
            return;
        }

        var rethrow = new boolean[1];

        try {
            var then = Values.getMember(env, obj, "then");

            if (then instanceof FunctionValue) Values.call(env, then, obj,
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
            else handle.onFulfil(obj);

            return;
        }
        catch (Exception e) {
            if (rethrow[0]) throw e;
        }

        handle.onFulfil(obj);
    }

    public static PromiseLib ofResolved(Environment ctx, Object value) {
        var res = new PromiseLib();
        res.fulfill(ctx, value);
        return res;
    }
    public static PromiseLib ofRejected(Environment ctx, EngineException value) {
        var res = new PromiseLib();
        res.reject(ctx, value);
        return res;
    }

    @Expose(value = "resolve", target = ExposeTarget.STATIC)
    public static PromiseLib __ofResolved(Arguments args) {
        return ofResolved(args.env, args.get(0));
    }
    @Expose(value = "reject", target = ExposeTarget.STATIC)
    public static PromiseLib __ofRejected(Arguments args) {
        return ofRejected(args.env, new EngineException(args.get(0)).setEnvironment(args.env));
    }

    @Expose(target = ExposeTarget.STATIC)
    public static PromiseLib __any(Arguments args) {
        if (!(args.get(0) instanceof ArrayValue)) throw EngineException.ofType("Expected argument for any to be an array.");
        var promises = args.convert(0, ArrayValue.class); 

        if (promises.size() == 0) return ofRejected(args.env, EngineException.ofError("No promises passed to 'Promise.any'.").setEnvironment(args.env));
        var n = new int[] { promises.size() };
        var res = new PromiseLib();
        var errors = new ArrayValue();

        for (var i = 0; i < promises.size(); i++) {
            var index = i;
            var val = promises.get(i);
            if (res.state != STATE_PENDING) break;

            handle(args.env, val, new Handle() {
                public void onFulfil(Object val) { res.fulfill(args.env, val); }
                public void onReject(EngineException err) {
                    errors.set(args.env, index, err.value);
                    n[0]--;
                    if (n[0] <= 0) res.reject(args.env, new EngineException(errors).setEnvironment(args.env));
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

            handle(args.env, val, new Handle() {
                @Override public void onFulfil(Object val) { res.fulfill(args.env, val); }
                @Override public void onReject(EngineException err) { res.reject(args.env, err); }
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

            handle(args.env, val, new Handle() {
                @Override public void onFulfil(Object val) {
                    result.set(args.env, index, val);
                    n[0]--;
                    if (n[0] <= 0) res.fulfill(args.env, result);
                }
                @Override public void onReject(EngineException err) {
                    res.reject(args.env, err);
                }
            });
        }

        if (n[0] <= 0) res.fulfill(args.env, result);

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

            handle(args.env, promises.get(i), new Handle() {
                @Override public void onFulfil(Object val) {
                    var desc = new ObjectValue();
                    desc.defineProperty(args.env, "status", "fulfilled");
                    desc.defineProperty(args.env, "value", val);

                    result.set(args.env, index, desc);

                    n[0]--;
                    if (n[0] <= 0) res.fulfill(args.env, res);
                }
                @Override public void onReject(EngineException err) {
                    var desc = new ObjectValue();
                    desc.defineProperty(args.env, "status", "reject");
                    desc.defineProperty(args.env, "value", err.value);

                    result.set(args.env, index, desc);

                    n[0]--;
                    if (n[0] <= 0) res.fulfill(args.env, res);
                }
            });
        }

        if (n[0] <= 0) res.fulfill(args.env, result);

        return res;
    }

    @Expose
    public static Object __then(Arguments args) {
        var onFulfill = args.get(0) instanceof FunctionValue ? args.convert(0, FunctionValue.class) : null;
        var onReject = args.get(1) instanceof FunctionValue ? args.convert(1, FunctionValue.class) : null;

        var res = new PromiseLib();

        handle(args.env, args.self, new Handle() {
            @Override public void onFulfil(Object val) {
                try { res.fulfill(args.env, onFulfill.call(args.env, null, val)); }
                catch (EngineException e) { res.reject(args.env, e); }
            }
            @Override public void onReject(EngineException err) {
                try { res.fulfill(args.env, onReject.call(args.env, null, err.value)); }
                catch (EngineException e) { res.reject(args.env, e); }
            }
        }.defer(args.env));

        return res;
    }
    @Expose
    public static Object __catch(Arguments args) {
        return __then(new Arguments(args.env, args.self, null, args.get(0)));
    }
    @Expose
    public static Object __finally(Arguments args) {
        var func = args.get(0) instanceof FunctionValue ? args.convert(0, FunctionValue.class) : null;

        var res = new PromiseLib();

        handle(args.env, args.self, new Handle() {
            @Override public void onFulfil(Object val) {
                try {
                    func.call(args.env);
                    res.fulfill(args.env, val);
                }
                catch (EngineException e) { res.reject(args.env, e); }
            }
            @Override public void onReject(EngineException err) {
                try {
                    func.call(args.env);
                    res.reject(args.env, err);
                }
                catch (EngineException e) { res.reject(args.env, e); }
            }
        }.defer(args.env));

        return res;
    }

    @ExposeConstructor
    public static PromiseLib __constructor(Arguments args) {
        var func = args.convert(0, FunctionValue.class);
        var res = new PromiseLib();

        try {
            func.call(
                args.env, null,
                new NativeFunction(null, _args -> {
                    res.fulfill(_args.env, _args.get(0));
                    return null;
                }),
                new NativeFunction(null, _args -> {
                    res.reject(_args.env, new EngineException(_args.get(0)).setEnvironment(_args.env));
                    return null;
                })
            );
        }
        catch (EngineException e) {
            res.reject(args.env, e);
        }

        return res;
    }
}
