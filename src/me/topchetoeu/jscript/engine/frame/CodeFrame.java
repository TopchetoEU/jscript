package me.topchetoeu.jscript.engine.frame;

import java.util.List;
import java.util.Stack;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.debug.DebugContext;
import me.topchetoeu.jscript.engine.scope.LocalScope;
import me.topchetoeu.jscript.engine.scope.ValueVariable;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.ScopeValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.exceptions.InterruptException;

public class CodeFrame {
    public static enum TryState {
        TRY,
        CATCH,
        FINALLY,
    }

    public static class TryCtx {
        public final int start, end, catchStart, finallyStart;
        public final int restoreStackPtr;
        public final TryState state;
        public final EngineException error;
        public final PendingResult result;

        public boolean hasCatch() { return catchStart >= 0; }
        public boolean hasFinally() { return finallyStart >= 0; }

        public boolean inBounds(int ptr) {
            return ptr >= start && ptr < end;
        }

        public void setCause(EngineException target) {
            if (error != null) target.setCause(error);
        }
        public TryCtx _catch(EngineException e) {
            return new TryCtx(TryState.CATCH, e, result, restoreStackPtr, start, end, -1, finallyStart);
        }
        public TryCtx _finally(PendingResult res) {
            return new TryCtx(TryState.FINALLY, error, res, restoreStackPtr, start, end, -1, -1);
        }

        public TryCtx(TryState state, EngineException err, PendingResult res, int stackPtr, int start, int end, int catchStart, int finallyStart) {
            this.catchStart = catchStart;
            this.finallyStart = finallyStart;
            this.restoreStackPtr = stackPtr;
            this.result = res == null ? PendingResult.ofNone() : res;
            this.state = state;
            this.start = start;
            this.end = end;
            this.error = err;
        }
    }

    private static class PendingResult {
        public final boolean isReturn, isJump, isThrow;
        public final Object value;
        public final EngineException error;
        public final int ptr;
        public final Instruction instruction;

        private PendingResult(Instruction instr, boolean isReturn, boolean isJump, boolean isThrow, Object value, EngineException error, int ptr) {
            this.instruction = instr;
            this.isReturn = isReturn;
            this.isJump = isJump;
            this.isThrow = isThrow;
            this.value = value;
            this.error = error;
            this.ptr = ptr;
        }

        public static PendingResult ofNone() {
            return new PendingResult(null, false, false, false, null, null, 0);
        }
        public static PendingResult ofReturn(Object value, Instruction instr) {
            return new PendingResult(instr, true, false, false, value, null, 0);
        }
        public static PendingResult ofThrow(EngineException error, Instruction instr) {
            return new PendingResult(instr, false, false, true, null, error, 0);
        }
        public static PendingResult ofJump(int codePtr, Instruction instr) {
            return new PendingResult(instr, false, true, false, null, null, codePtr);
        }
    }

    public final LocalScope scope;
    public final Object thisArg;
    public final Object[] args;
    public final Stack<TryCtx> tryStack = new Stack<>();
    public final CodeFunction function;
    public final Context ctx;
    public Object[] stack = new Object[32];
    public int stackPtr = 0;
    public int codePtr = 0;
    public boolean jumpFlag = false, popTryFlag = false;
    private Location prevLoc = null;

    public ObjectValue getLocalScope(Context ctx, boolean props) {
        var names = new String[scope.locals.length];

        for (int i = 0; i < scope.locals.length; i++) {
            var name = "local_" + (i - 2);

            if (i == 0) name = "this";
            else if (i == 1) name = "arguments";
            else if (i < function.localNames.length) name = function.localNames[i];

            names[i] = name;
        }

        return new ScopeValue(scope.locals, names);
    }
    public ObjectValue getCaptureScope(Context ctx, boolean props) {
        var names = new String[scope.captures.length];

        for (int i = 0; i < scope.captures.length; i++) {
            var name = "capture_" + (i - 2);
            if (i < function.captureNames.length) name = function.captureNames[i];
            names[i] = name;
        }

        return new ScopeValue(scope.captures, names);
    }
    public ObjectValue getValStackScope(Context ctx) {
        return new ObjectValue() {
            @Override
            protected Object getField(Context ctx, Object key) {
                var i = (int)Values.toNumber(ctx, key);
                if (i < 0 || i >= stackPtr) return null;
                else return stack[i];
            }
            @Override
            protected boolean hasField(Context ctx, Object key) {
                return true;
            }
            @Override
            public List<Object> keys(boolean includeNonEnumerable) {
                var res = super.keys(includeNonEnumerable);
                for (var i = 0; i < stackPtr; i++) res.add(i);
                return res;
            }
        };
    }

    public void addTry(int start, int end, int catchStart, int finallyStart) {
        var err = tryStack.empty() ? null : tryStack.peek().error;
        var res = new TryCtx(TryState.TRY, err, null, stackPtr, start, end, catchStart, finallyStart);

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

    public Object next(Object value, Object returnValue, EngineException error) {
        if (value != Runners.NO_RETURN) push(ctx, value);

        Instruction instr = null;
        if (codePtr >= 0 && codePtr < function.body.length) instr = function.body[codePtr];

        if (returnValue == Runners.NO_RETURN && error == null) {
            try {
                if (Thread.currentThread().isInterrupted()) throw new InterruptException();

                if (instr == null) returnValue = null;
                else {
                    DebugContext.get(ctx).onInstruction(ctx, this, instr, Runners.NO_RETURN, null, false);

                    if (instr.location != null) prevLoc = instr.location;

                    try {
                        this.jumpFlag = this.popTryFlag = false;
                        returnValue = Runners.exec(ctx, instr, this);
                    }
                    catch (EngineException e) {
                        error = e.add(ctx, function.name, prevLoc);
                    }
                }
            }
            catch (EngineException e) { error = e; }
        }

        while (!tryStack.empty()) {
            var tryCtx = tryStack.peek();
            TryCtx newCtx = null;

            if (error != null) {
                tryCtx.setCause(error);
                if (tryCtx.hasCatch()) newCtx = tryCtx._catch(error);
                else if (tryCtx.hasFinally()) newCtx = tryCtx._finally(PendingResult.ofThrow(error, instr));
            }
            else if (returnValue != Runners.NO_RETURN) {
                if (tryCtx.hasFinally()) newCtx = tryCtx._finally(PendingResult.ofReturn(returnValue, instr));
            }
            else if (jumpFlag && !tryCtx.inBounds(codePtr)) {
                if (tryCtx.hasFinally()) newCtx = tryCtx._finally(PendingResult.ofJump(codePtr, instr));
            }
            else if (!this.popTryFlag) newCtx = tryCtx;

            if (newCtx != null) {
                if (newCtx != tryCtx) {
                    switch (newCtx.state) {
                        case CATCH:
                            if (tryCtx.state != TryState.CATCH) scope.catchVars.add(new ValueVariable(false, error.value));
                            codePtr = tryCtx.catchStart;
                            stackPtr = tryCtx.restoreStackPtr;
                            break;
                        case FINALLY:
                            if (tryCtx.state == TryState.CATCH) scope.catchVars.remove(scope.catchVars.size() - 1);
                            codePtr = tryCtx.finallyStart;
                            stackPtr = tryCtx.restoreStackPtr;
                        default:
                    }

                    tryStack.pop();
                    tryStack.push(newCtx);
                }
                error = null;
                returnValue = Runners.NO_RETURN;
                break;
            }
            else {
                popTryFlag = false;
                if (tryCtx.state == TryState.CATCH) scope.catchVars.remove(scope.catchVars.size() - 1);

                if (tryCtx.state != TryState.FINALLY && tryCtx.hasFinally()) {
                    codePtr = tryCtx.finallyStart;
                    stackPtr = tryCtx.restoreStackPtr;
                    tryStack.pop();
                    tryStack.push(tryCtx._finally(null));
                    break;
                }
                else {
                    tryStack.pop();
                    codePtr = tryCtx.end;
                    if (tryCtx.result.instruction != null) instr = tryCtx.result.instruction;
                    if (!jumpFlag && returnValue == Runners.NO_RETURN && error == null) {
                        if (tryCtx.result.isJump) {
                            codePtr = tryCtx.result.ptr;
                            jumpFlag = true;
                        }
                        if (tryCtx.result.isReturn) returnValue = tryCtx.result.value;
                        if (error == null && tryCtx.result.isThrow) {
                            error = tryCtx.result.error;
                        }
                    }
                    if (error != null) tryCtx.setCause(error);
                    continue;
                }
            }
        }

        if (error != null) {
            var caught = false;

            for (var frame : ctx.frames()) {
                for (var tryCtx : frame.tryStack) {
                    if (tryCtx.state == TryState.TRY) caught = true;
                }
            }

            DebugContext.get(ctx).onInstruction(ctx, this, instr, null, error, caught);
            throw error;
        }
        if (returnValue != Runners.NO_RETURN) {
            DebugContext.get(ctx).onInstruction(ctx, this, instr, returnValue, null, false);
            return returnValue;
        }

        return Runners.NO_RETURN;
    }

    public void onPush() {
        DebugContext.get(ctx).onFramePush(ctx, this);
    }
    public void onPop() {
        DebugContext.get(ctx.parent).onFramePop(ctx.parent, this);
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
        this.ctx = ctx.pushFrame(this);
    }
}
