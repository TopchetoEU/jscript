package me.topchetoeu.jscript.runtime;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.CancellationException;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.CodeFunction;
import me.topchetoeu.jscript.runtime.values.objects.ArrayLikeValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;

public final class Frame {
    public static final Key<Frame> KEY = Key.of();

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
        public final Value value;
        public final EngineException error;
        public final int ptr;
        public final Instruction instruction;

        private PendingResult(Instruction instr, boolean isReturn, boolean isJump, boolean isThrow, Value value, EngineException error, int ptr) {
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
        public static PendingResult ofReturn(Value value, Instruction instr) {
            return new PendingResult(instr, true, false, false, value, null, 0);
        }
        public static PendingResult ofThrow(EngineException error, Instruction instr) {
            return new PendingResult(instr, false, false, true, null, error, 0);
        }
        public static PendingResult ofJump(int codePtr, Instruction instr) {
            return new PendingResult(instr, false, true, false, null, null, codePtr);
        }
    }

    /**
     * A list of one-element arrays of values. This is so that we can pass captures to other functions
     */
    public final Value[][] captures;
    public final Value[] locals;
    public final Value[][] capturables;
    public final Value argsVal;
    public Value self;
    public Value fakeArgs;
    public final Value[] args;
    public final boolean isNew;
    public final Stack<TryCtx> tryStack = new Stack<>();
    public final CodeFunction function;
    public final Environment env;
    private final DebugContext dbg;

    public Value getVar(int i) {
        if (i < 0) return captures[~i][0];
        else if (i < locals.length) return locals[i];
        else return capturables[i - locals.length][0];
    }
    public Value setVar(int i, Value val) {
        if (i < 0) return captures[~i][0] = val;
        else if (i < locals.length) return locals[i] = val;
        else return capturables[i - locals.length][0] = val;
    }
    public Value[] captureVar(int i) {
        if (i < 0) return captures[~i];
        if (i >= locals.length) return capturables[i - locals.length];
        else throw new RuntimeException("Illegal capture");
    }

    public Value[] stack = new Value[32];
    public int stackPtr = 0;
    public int codePtr = 0;
    public boolean jumpFlag = false;
    public boolean popTryFlag = false;

    public void addTry(int start, int end, int catchStart, int finallyStart) {
        var err = tryStack.empty() ? null : tryStack.peek().error;
        var res = new TryCtx(TryState.TRY, err, null, stackPtr, start, end, catchStart, finallyStart);

        tryStack.add(res);
    }

    public Value peek() {
        return peek(0);
    }
    public Value peek(int offset) {
        return stack[stackPtr - 1 - offset];
    }
    public Value pop() {
        return stack[--stackPtr];
    }
    public Value[] take(int n) {
        Value[] res = new Value[n];
        System.arraycopy(stack, stackPtr - n, res, 0, n);
        stackPtr -= n;

        return res;
    }
    public void push(Value val) {
        if (stack.length <= stackPtr) {
            var newStack = new Value[stack.length * 2];
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            stack = newStack;
        }

        stack[stackPtr++] = val;
    }
    public void replace(Value val) {
        stack[stackPtr - 1] = val;
    }

    // for the love of christ don't touch this
    /**
     * This is provided only for optimization-sike. All parameters must be null except at most one, otherwise undefined behavior
     */
    public final Value next(Value value, Value returnValue, EngineException error) {
        if (value != null) push(value);

        Instruction instr = null;
        if (codePtr != function.body.instructions.length) instr = function.body.instructions[codePtr];

        if (returnValue == null && error == null) {
            try {
                if (Thread.interrupted()) throw new CancellationException();

                if (instr == null) {
                    if (stackPtr > 0) returnValue = stack[stackPtr - 1];
                    else returnValue = Value.UNDEFINED;
                }
                else {
                    dbg.onInstruction(env, this, instr);

                    try {
                        this.jumpFlag = this.popTryFlag = false;
                        returnValue = InstructionRunner.exec(env, instr, this);
                    }
                    catch (EngineException e) {
                        error = e.add(env, function.name, dbg.getMapOrEmpty(function).toLocation(codePtr, true));
                    }
                }
            }
            catch (EngineException e) { error = e; }
            catch (RuntimeException e) {
                System.out.println(dbg.getMapOrEmpty(function).toLocation(codePtr, true));
                throw e;
            }
        }

        while (!tryStack.empty()) {
            var tryCtx = tryStack.peek();
            TryCtx newCtx = null;

            if (error != null) {
                tryCtx.setCause(error);
                if (tryCtx.hasCatch()) newCtx = tryCtx._catch(error);
                else if (tryCtx.hasFinally()) newCtx = tryCtx._finally(PendingResult.ofThrow(error, instr));
            }
            else if (returnValue != null) {
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
                            codePtr = tryCtx.catchStart;
                            stackPtr = tryCtx.restoreStackPtr;
                            break;
                        case FINALLY:
                            codePtr = tryCtx.finallyStart;
                            stackPtr = tryCtx.restoreStackPtr;
                        default:
                    }

                    tryStack.pop();
                    tryStack.push(newCtx);
                }

                error = null;
                returnValue = null;
                break;
            }
            else {
                popTryFlag = false;

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
                    if (!jumpFlag && returnValue == null && error == null) {
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

            for (var frame : dbg.getStackFrames()) {
                for (var tryCtx : frame.tryStack) {
                    if (tryCtx.state == TryState.TRY) caught = true;
                }
            }

            dbg.onInstruction(env, this, instr, null, error, caught);
            throw error;
        }
        if (returnValue != null) {
            dbg.onInstruction(env, this, instr, returnValue, null, false);
            return returnValue;
        }

        return null;
    }

    /**
     * Executes the next instruction in the frame
     */
    public final Value next() {
        return next(null, null, null);
    }
    /**
     * Induces a value on the stack (as if it were returned by the last function call)
     * and executes the next instruction in the frame.
     * 
     * @param value The value to induce
     */
    public final Value next(Value value) {
        return next(value, null, null);
    }
    /**
     * Induces a thrown error and executes the next instruction.
     * Note that this is different than just throwing the error outside the
     * function, as the function executed could have a try-catch which
     * would otherwise handle the error
     * 
     * @param error The error to induce
     */
    public final Value induceError(EngineException error) {
        return next(null, null, error);
    }
    /**
     * Induces a return, as if there was a return statement before
     * the currently executed instruction and executes the next instruction.
     * Note that this is different than just returning the value outside the
     * function, as the function executed could have a try-catch which
     * would otherwise handle the error
     * 
     * @param value The retunr value to induce
     */
    public final Value induceReturn(Value value) {
        return next(null, value, null);
    }

    public void onPush() {
        DebugContext.get(env).onFramePush(env, this);
    }
    public void onPop() {
        DebugContext.get(env).onFramePop(env, this);
    }

    /**
     * Gets an array proxy of the local locals
     */
    public ObjectValue getValStackScope() {
        return new ArrayLikeValue() {
            @Override public Value get(int i) { return stack[i]; }
            @Override public boolean set(Environment env, int i, Value val) { return false; }
            @Override public boolean has(int i) { return i >= 0 && i < size(); }
            @Override public boolean remove(int i) { return false; }

            @Override public int size() { return stackPtr; }
            @Override public boolean setSize(int val) { return false; }
        };
    }

    public Frame(Environment env, boolean isNew, Value thisArg, Value[] args, CodeFunction func) {
        this.env = env;
        this.dbg = DebugContext.get(env);
        this.function = func;
        this.isNew = isNew;

        this.self = thisArg;
        this.args = args;
        this.argsVal = new ArgumentsValue(this, args);
        this.captures = func.captures;

        var i = 0;

        this.locals = new Value[func.body.localsN];
        Arrays.fill(locals, Value.UNDEFINED);

        this.capturables = new Value[func.body.capturablesN][1];

        for (i = 0; i < func.body.capturablesN; i++) {
            this.capturables[i][0] = Value.UNDEFINED;
        }
    }
}
