package me.topchetoeu.jscript.runtime;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.environment.Key;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.exceptions.InterruptException;
import me.topchetoeu.jscript.runtime.scope.LocalScope;
import me.topchetoeu.jscript.runtime.scope.ValueVariable;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.functions.CodeFunction;
import me.topchetoeu.jscript.runtime.values.objects.ArrayValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.objects.ScopeValue;
import me.topchetoeu.jscript.runtime.values.primitives.VoidValue;

public class Frame {
    public static final Key<Frame> KEY = new Key<>();

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

    public final LocalScope scope;
    public final Object thisArg;
    public final Object[] args;
    public final Stack<TryCtx> tryStack = new Stack<>();
    public final CodeFunction function;
    public final Environment env;

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
        if (stackPtr <= offset) return null;
        else return stack[stackPtr - 1 - offset];
    }
    public Value pop() {
        if (stackPtr == 0) return VoidValue.UNDEFINED;
        return stack[--stackPtr];
    }
    public Value[] take(int n) {
        int srcI = stackPtr - n;
        if (srcI < 0) srcI = 0;

        int dstI = n + srcI - stackPtr;
        int copyN = stackPtr - srcI;

        Value[] res = new Value[n];
        Arrays.fill(res, VoidValue.UNDEFINED);
        System.arraycopy(stack, srcI, res, dstI, copyN);
        stackPtr -= copyN;

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

    // for the love of christ don't touch this
    private Value next(Value value, Value returnValue, EngineException error) {
        if (value != null) push(value);

        Instruction instr = null;
        if (codePtr >= 0 && codePtr < function.body.instructions.length) instr = function.body.instructions[codePtr];

        if (returnValue == null && error == null) {
            try {
                if (Thread.interrupted()) throw new InterruptException();

                if (instr == null) returnValue = null;
                else {
                    DebugContext.get(env).onInstruction(env, this, instr, null, null, false);

                    try {
                        this.jumpFlag = this.popTryFlag = false;
                        returnValue = InstructionRunner.exec(env, instr, this);
                    }
                    catch (EngineException e) {
                        error = e.add(env, function.name, DebugContext.get(env).getMapOrEmpty(function).toLocation(codePtr, true));
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
                returnValue = null;
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

            for (var frame : DebugContext.get(env).getStackFrames()) {
                for (var tryCtx : frame.tryStack) {
                    if (tryCtx.state == TryState.TRY) caught = true;
                }
            }

            DebugContext.get(env).onInstruction(env, this, instr, null, error, caught);
            throw error;
        }
        if (returnValue != null) {
            DebugContext.get(env).onInstruction(env, this, instr, returnValue, null, false);
            return returnValue;
        }

        return null;
    }

    /**
     * Executes the next instruction in the frame
     */
    public Value next() {
        return next(null, null, null);
    }
    /**
     * Induces a value on the stack (as if it were returned by the last function call)
     * and executes the next instruction in the frame.
     * 
     * @param value The value to induce
     */
    public Value next(Value value) {
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
    public Value induceError(EngineException error) {
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
    public Value induceReturn(Value value) {
        return next(null, value, null);
    }

    public void onPush() {
        DebugContext.get(env).onFramePush(env, this);
    }
    public void onPop() {
        DebugContext.get(env).onFramePop(env, this);
    }

    /**
     * Gets an object proxy of the local scope
     */
    public ObjectValue getLocalScope() {
        var names = new String[scope.locals.length];
        var map = DebugContext.get(env).getMapOrEmpty(function);

        for (int i = 0; i < scope.locals.length; i++) {
            var name = "local_" + (i - 2);

            if (i == 0) name = "this";
            else if (i == 1) name = "arguments";
            else if (i < map.localNames.length) name = map.localNames[i];

            names[i] = name;
        }

        return new ScopeValue(scope.locals, names);
    }
    /**
     * Gets an object proxy of the capture scope
     */
    public ObjectValue getCaptureScope() {
        var names = new String[scope.captures.length];
        var map = DebugContext.get(env).getMapOrEmpty(function);

        for (int i = 0; i < scope.captures.length; i++) {
            var name = "capture_" + (i - 2);
            if (i < map.captureNames.length) name = map.captureNames[i];
            names[i] = name;
        }

        return new ScopeValue(scope.captures, names);
    }
    /**
     * Gets an array proxy of the local scope
     */
    public ObjectValue getValStackScope() {
        return new ObjectValue() {
            @Override public Member getOwnMember(Environment env, Value key) {
                var res = super.getOwnMember(env, key);
                if (res != null) return res;

                var f = key.toNumber(env).value;
                var i = (int)f;

                if (i < 0 || i >= stackPtr) return null;
                else return new FieldMember(false, true, true) {
                    @Override public Value get(Environment env, Value self) { return stack[i]; }
                    @Override public boolean set(Environment env, Value val, Value self) {
                        stack[i] = val;
                        return true;
                    }
                };
            }
            @Override public Map<String, Member> getOwnMembers(Environment env) {
                var res = new LinkedHashMap<String, Member>();

                for (var i = 0; i < stackPtr; i++) {
                    var _i = i;
                    res.put(i + "", new FieldMember(false, true, true) {
                        @Override public Value get(Environment env, Value self) { return stack[_i]; }
                        @Override public boolean set(Environment env, Value val, Value self) {
                            stack[_i] = val;
                            return true;
                        }
                    });
                }

                return res;
            }
        };
    }

    public Frame(Environment env, Value thisArg, Value[] args, CodeFunction func) {
        this.env = env;
        this.args = args.clone();
        this.scope = new LocalScope(func.body.localsN, func.captures);
        this.scope.get(0).set(thisArg);
        this.scope.get(1).value = new ArrayValue(args);

        this.thisArg = thisArg;
        this.function = func;
    }
}
