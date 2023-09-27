package me.topchetoeu.jscript.engine.frame;

import java.util.Stack;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.scope.LocalScope;
import me.topchetoeu.jscript.engine.scope.ValueVariable;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
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
        public Object err;
        public int jumpPtr;

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
    public final Stack<TryCtx> tryStack = new Stack<>();
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

    private void setCause(Context ctx, Object err, Object cause) throws InterruptedException {
        if (err instanceof ObjectValue) {
            Values.setMember(ctx, err, ctx.env.symbol("Symbol.cause"), cause);
        }
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
            throw e.add(function.name, prevLoc).setContext(ctx);
        }
    }

    public Object next(Context ctx, Object value, Object returnValue, Object error) throws InterruptedException {
        if (value != Runners.NO_RETURN) push(ctx, value);

        if (returnValue == Runners.NO_RETURN && error == Runners.NO_RETURN) {
            try { returnValue = nextNoTry(ctx); }
            catch (EngineException e) { error = e.value; }
        }

        while (!tryStack.empty()) {
            var tryCtx = tryStack.peek();
            var newState = -1;

            switch (tryCtx.state) {
                case TryCtx.STATE_TRY:
                    if (error != Runners.NO_RETURN) {
                        if (tryCtx.hasCatch) {
                            tryCtx.err = error;
                            newState = TryCtx.STATE_CATCH;
                        }
                        else if (tryCtx.hasFinally) {
                            tryCtx.err = error;
                            newState = TryCtx.STATE_FINALLY_THREW;
                        }
                        break;
                    }
                    else if (returnValue != Runners.NO_RETURN) {
                        if (tryCtx.hasFinally) {
                            tryCtx.retVal = error;
                            newState = TryCtx.STATE_FINALLY_RETURNED;
                        }
                        break;
                    }
                    else if (codePtr >= tryCtx.tryStart && codePtr < tryCtx.catchStart) return Runners.NO_RETURN;

                    if (tryCtx.hasFinally) {
                        if (jumpFlag) tryCtx.jumpPtr = codePtr;
                        else tryCtx.jumpPtr = tryCtx.end;
                        newState = TryCtx.STATE_FINALLY_JUMPED;
                    }
                    else codePtr = tryCtx.end;
                    break;
                case TryCtx.STATE_CATCH:
                    if (error != Runners.NO_RETURN) {
                        if (tryCtx.hasFinally) {
                            tryCtx.err = error;
                            newState = TryCtx.STATE_FINALLY_THREW;
                        }
                        break;
                    }
                    else if (returnValue != Runners.NO_RETURN) {
                        if (tryCtx.hasFinally) {
                            tryCtx.retVal = error;
                            newState = TryCtx.STATE_FINALLY_RETURNED;
                        }
                        break;
                    }
                    else if (codePtr >= tryCtx.catchStart && codePtr < tryCtx.finallyStart) return Runners.NO_RETURN;

                    if (tryCtx.hasFinally) {
                        if (jumpFlag) tryCtx.jumpPtr = codePtr;
                        else tryCtx.jumpPtr = tryCtx.end;
                        newState = TryCtx.STATE_FINALLY_JUMPED;
                    }
                    else codePtr = tryCtx.end;
                    break;
                case TryCtx.STATE_FINALLY_THREW:
                    if (error != Runners.NO_RETURN) setCause(ctx, error, tryCtx.err);
                    else if (codePtr < tryCtx.finallyStart || codePtr >= tryCtx.end) error = tryCtx.err;
                    else return Runners.NO_RETURN;
                    break;
                case TryCtx.STATE_FINALLY_RETURNED:
                    if (returnValue == Runners.NO_RETURN) {
                        if (codePtr < tryCtx.finallyStart || codePtr >= tryCtx.end) returnValue = tryCtx.retVal;
                        else return Runners.NO_RETURN;
                    }
                    break;
                case TryCtx.STATE_FINALLY_JUMPED:
                    if (codePtr < tryCtx.finallyStart || codePtr >= tryCtx.end) {
                        if (!jumpFlag) codePtr = tryCtx.jumpPtr;
                        else codePtr = tryCtx.end;
                    }
                    else return Runners.NO_RETURN;
                    break;
            }

            if (tryCtx.state == TryCtx.STATE_CATCH) scope.catchVars.remove(scope.catchVars.size() - 1);

            if (newState == -1) {
                tryStack.pop();
                continue;
            }

            tryCtx.state = newState;
            switch (newState) {
                case TryCtx.STATE_CATCH:
                    scope.catchVars.add(new ValueVariable(false, tryCtx.err));
                    codePtr = tryCtx.catchStart;
                    break;
                default:
                    codePtr = tryCtx.finallyStart;
            }

            return Runners.NO_RETURN;
        }
    
        if (error != Runners.NO_RETURN) throw new EngineException(error);
        if (returnValue != Runners.NO_RETURN) return returnValue;
        return Runners.NO_RETURN;
    }

    public Object run(Context ctx) throws InterruptedException {
        try {
            ctx.message.pushFrame(ctx, this);
            while (true) {
                var res = next(ctx, Runners.NO_RETURN, Runners.NO_RETURN, Runners.NO_RETURN);
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
