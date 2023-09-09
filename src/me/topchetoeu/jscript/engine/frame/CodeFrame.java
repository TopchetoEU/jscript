package me.topchetoeu.jscript.engine.frame;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.scope.LocalScope;
import me.topchetoeu.jscript.engine.scope.ValueVariable;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;

public class CodeFrame {
    private class TryCtx {
        public static final int STATE_TRY = 0;
        public static final int STATE_CATCH = 1;
        public static final int STATE_FINALLY_THREW = 2;
        public static final int STATE_FINALLY_RETURNED = 3;
        public static final int STATE_FINALLY_JUMPED = 4;

        public final boolean hasCatch, hasFinally;
        public final int tryStart, catchStart, finallyStart, end;
        public int state;
        public Object retVal;
        public int jumpPtr;
        public EngineException err;

        public TryCtx(int tryStart, int tryN, int catchN, int finallyN) {
            hasCatch = catchN >= 0;
            hasFinally = finallyN >= 0;

            if (catchN < 0) catchN = 0;
            if (finallyN < 0) finallyN = 0;

            this.tryStart = tryStart;
            this.catchStart = tryStart + tryN;
            this.finallyStart = catchStart + catchN;
            this.end = finallyStart + finallyN;
            this.jumpPtr = end;
        }
    }

    public final LocalScope scope;
    public final Object thisArg;
    public final Object[] args;
    public final List<TryCtx> tryStack = new ArrayList<>();
    public final CodeFunction function;

    public Object[] stack = new Object[32];
    public int stackPtr = 0;
    public int codePtr = 0;
    public boolean jumpFlag = false;
    private Location prevLoc = null;

    public void addTry(int n, int catchN, int finallyN) {
        var res = new TryCtx(codePtr + 1, n, catchN, finallyN);

        tryStack.add(res);
    }

    public Object peek() {
        return peek(0);
    }
    public Object peek(int offset) {
        if (stackPtr <= offset) return null;
        else return stack[stackPtr - 1 - offset];
    }
    public Object pop() {
        if (stackPtr == 0) return null;
        return stack[--stackPtr];
    }
    public Object[] take(int n) {
        int srcI = stackPtr - n;
        if (srcI < 0) srcI = 0;

        int dstI = n + srcI - stackPtr;
        int copyN = stackPtr - srcI;

        Object[] res = new Object[n];
        System.arraycopy(stack, srcI, res, dstI, copyN);
        stackPtr -= copyN;

        return res;
    }
    public void push(Context ctx, Object val) {
        if (stack.length <= stackPtr) {
            var newStack = new Object[stack.length * 2];
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            stack = newStack;
        }
        stack[stackPtr++] = Values.normalize(ctx, val);
    }

    private Object nextNoTry(Context ctx) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        if (codePtr < 0 || codePtr >= function.body.length) return null;

        var instr = function.body[codePtr];

        var loc = instr.location;
        if (loc != null) prevLoc = loc;

        try {
            this.jumpFlag = false;
            return Runners.exec(ctx, instr, this);
        }
        catch (EngineException e) {
            throw e.add(function.name, prevLoc);
        }
    }

    public Object next(Context ctx, Object prevReturn, Object prevError) throws InterruptedException {
        TryCtx tryCtx = null;
        if (prevError != Runners.NO_RETURN) prevReturn = Runners.NO_RETURN;

        while (!tryStack.isEmpty()) {
            var tmp = tryStack.get(tryStack.size() - 1);
            var remove = false;

            if (prevError != Runners.NO_RETURN) {
                remove = true;
                if (tmp.state == TryCtx.STATE_TRY) {
                    tmp.jumpPtr = tmp.end;

                    if (tmp.hasCatch) {
                        tmp.state = TryCtx.STATE_CATCH;
                        scope.catchVars.add(new ValueVariable(false, prevError));
                        prevError = Runners.NO_RETURN;
                        codePtr = tmp.catchStart;
                        remove = false;
                    }
                    else if (tmp.hasFinally) {
                        tmp.state = TryCtx.STATE_FINALLY_THREW;
                        tmp.err = new EngineException(prevError);
                        prevError = Runners.NO_RETURN;
                        codePtr = tmp.finallyStart;
                        remove = false;
                    }
                }
            }
            else if (prevReturn != Runners.NO_RETURN) {
                remove = true;
                if (tmp.hasFinally && tmp.state <= TryCtx.STATE_CATCH) {
                    tmp.state = TryCtx.STATE_FINALLY_RETURNED;
                    tmp.retVal = prevReturn;
                    prevReturn = Runners.NO_RETURN;
                    codePtr = tmp.finallyStart;
                    remove = false;
                }
            }
            else if (tmp.state == TryCtx.STATE_TRY) {
                if (codePtr < tmp.tryStart || codePtr >= tmp.catchStart) {
                    if (jumpFlag) tmp.jumpPtr = codePtr;
                    else tmp.jumpPtr = tmp.end;

                    if (tmp.hasFinally) {
                        tmp.state = TryCtx.STATE_FINALLY_JUMPED;
                        codePtr = tmp.finallyStart;
                    }
                    else codePtr = tmp.jumpPtr;
                    remove = !tmp.hasFinally;
                }
            }
            else if (tmp.state == TryCtx.STATE_CATCH) {
                if (codePtr < tmp.catchStart || codePtr >= tmp.finallyStart) {
                    if (jumpFlag) tmp.jumpPtr = codePtr;
                    else tmp.jumpPtr = tmp.end;
                    scope.catchVars.remove(scope.catchVars.size() - 1);

                    if (tmp.hasFinally) {
                        tmp.state = TryCtx.STATE_FINALLY_JUMPED;
                        codePtr = tmp.finallyStart;
                    }
                    else codePtr = tmp.jumpPtr;
                    remove = !tmp.hasFinally;
                }
            }
            else if (codePtr < tmp.finallyStart || codePtr >= tmp.end) {
                if (!jumpFlag) {
                    if (tmp.state == TryCtx.STATE_FINALLY_THREW) throw tmp.err;
                    else if (tmp.state == TryCtx.STATE_FINALLY_RETURNED) return tmp.retVal;
                    else if (tmp.state == TryCtx.STATE_FINALLY_JUMPED) codePtr = tmp.jumpPtr;
                }
                else codePtr = tmp.jumpPtr;
                remove = true;
            }

            if (remove) tryStack.remove(tryStack.size() - 1);
            else {
                tryCtx = tmp;
                break;
            }
        }

        if (prevError != Runners.NO_RETURN) throw new EngineException(prevError);
        if (prevReturn != Runners.NO_RETURN) return prevReturn;

        if (tryCtx == null) return nextNoTry(ctx);
        else if (tryCtx.state == TryCtx.STATE_TRY) {
            try {
                var res = nextNoTry(ctx);
                if (res != Runners.NO_RETURN && tryCtx.hasFinally) {
                    tryCtx.retVal = res;
                    tryCtx.state = TryCtx.STATE_FINALLY_RETURNED;
                }

                else return res;
            }
            catch (EngineException e) {
                if (tryCtx.hasCatch) {
                    tryCtx.state = TryCtx.STATE_CATCH;
                    tryCtx.err = e;
                    codePtr = tryCtx.catchStart;
                    scope.catchVars.add(new ValueVariable(false, e.value));
                    return Runners.NO_RETURN;
                }
                else if (tryCtx.hasFinally) {
                    tryCtx.err = e;
                    tryCtx.state = TryCtx.STATE_FINALLY_THREW;
                }
                else throw e;
            }

            codePtr = tryCtx.finallyStart;
            return Runners.NO_RETURN;
        }
        else if (tryCtx.state == TryCtx.STATE_CATCH) {
            try {
                var res = nextNoTry(ctx);
                if (res != Runners.NO_RETURN && tryCtx.hasFinally) {
                    tryCtx.retVal = res;
                    tryCtx.state = TryCtx.STATE_FINALLY_RETURNED;
                }
                else return res;
            }
            catch (EngineException e) {
                e.cause = tryCtx.err;
                if (tryCtx.hasFinally) {
                    tryCtx.err = e;
                    tryCtx.state = TryCtx.STATE_FINALLY_THREW;
                }
                else throw e;
            }

            codePtr = tryCtx.finallyStart;
            return Runners.NO_RETURN;
        }
        else if (tryCtx.state == TryCtx.STATE_FINALLY_THREW) {
            try {
                return nextNoTry(ctx);
            }
            catch (EngineException e) {
                e.cause = tryCtx.err;
                throw e;
            }
        }
        else return nextNoTry(ctx);
    }

    public Object run(Context ctx) throws InterruptedException {
        try {
            ctx.message.pushFrame(this);
            while (true) {
                var res = next(ctx, Runners.NO_RETURN, Runners.NO_RETURN);
                if (res != Runners.NO_RETURN) return res;
            }
        }
        finally {
            ctx.message.popFrame(this);
        }
    }

    public CodeFrame(Context ctx, Object thisArg, Object[] args, CodeFunction func) {
        this.args = args.clone();
        this.scope = new LocalScope(func.localsN, func.captures);
        this.scope.get(0).set(null, thisArg);
        var argsObj = new ArrayValue();
        for (var i = 0; i < args.length; i++) {
            argsObj.set(ctx, i, args[i]);
        }
        this.scope.get(1).value = argsObj;

        this.thisArg = thisArg;
        this.function = func;
    }
}
