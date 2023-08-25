package me.topchetoeu.jscript.engine.frame;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.DebugCommand;
import me.topchetoeu.jscript.engine.Engine;
import me.topchetoeu.jscript.engine.CallContext.DataKey;
import me.topchetoeu.jscript.engine.scope.LocalScope;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.CodeFunction;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;

public class CodeFrame {
    private record TryCtx(int tryStart, int tryEnd, int catchStart, int catchEnd, int finallyStart, int finallyEnd) { }

    public static final DataKey<Integer> STACK_N_KEY = new DataKey<>();
    public static final DataKey<Integer> MAX_STACK_KEY = new DataKey<>();
    public static final DataKey<Boolean> STOP_AT_START_KEY = new DataKey<>();
    public static final DataKey<Boolean> STEPPING_TROUGH_KEY = new DataKey<>();

    public final LocalScope scope;
    public final Object thisArg;
    public final Object[] args;
    public final List<TryCtx> tryStack = new ArrayList<>();
    public final CodeFunction function;

    public Object[] stack = new Object[32];
    public int stackPtr = 0;
    public int codePtr = 0;
    private DebugCommand debugCmd = null;
    private Location prevLoc = null;

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
    public void push(Object val) {
        if (stack.length <= stackPtr) {
            var newStack = new Object[stack.length * 2];
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            stack = newStack;
        }
        stack[stackPtr++] = Values.normalize(val);
    }

    public void start(CallContext ctx) {
        if (ctx.getData(STACK_N_KEY, 0) >= ctx.addData(MAX_STACK_KEY, 10000)) throw EngineException.ofRange("Stack overflow!");
        ctx.changeData(STACK_N_KEY);

        var debugState = ctx.getData(Engine.DEBUG_STATE_KEY);
        if (debugState != null) debugState.pushFrame(this);
    }
    public void end(CallContext ctx) {
        var debugState = ctx.getData(Engine.DEBUG_STATE_KEY);

        if (debugState != null) debugState.popFrame();
        ctx.changeData(STACK_N_KEY, -1);
    }

    private Object nextNoTry(CallContext ctx) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        if (codePtr < 0 || codePtr >= function.body.length) return null;

        var instr = function.body[codePtr];

        var loc = instr.location;
        if (loc != null) prevLoc = loc;

        // var debugState = ctx.getData(Engine.DEBUG_STATE_KEY);
        // if (debugCmd == null) {
        //     if (ctx.getData(STOP_AT_START_KEY, false)) debugCmd = DebugCommand.STEP_OVER;
        //     else debugCmd = DebugCommand.NORMAL;

        //     if (debugState != null) debugState.pushFrame(this);
        // }

        // if (debugState != null && loc != null) {
        //     if (
        //         instr.type == Type.NOP && instr.match("debug") || debugState.breakpoints.contains(loc) || (
        //             ctx.getData(STEPPING_TROUGH_KEY, false) &&
        //             (debugCmd == DebugCommand.STEP_INTO || debugCmd == DebugCommand.STEP_OVER)
        //         )
        //     ) {
        //         ctx.setData(STEPPING_TROUGH_KEY, true);

        //         debugState.breakpointNotifier.next(new BreakpointData(loc, ctx));
        //         debugCmd = debugState.commandNotifier.toAwaitable().await();
        //         if (debugCmd == DebugCommand.NORMAL) ctx.setData(STEPPING_TROUGH_KEY, false);
        //     }
        // }

        try {
            return Runners.exec(debugCmd, instr, this, ctx);
        }
        catch (EngineException e) {
            throw e.add(function.name, prevLoc);
        }
    }

    public Object next(CallContext ctx) throws InterruptedException {
        return nextNoTry(ctx);
    }

    public Object run(CallContext ctx) throws InterruptedException {
        try {
            start(ctx);
            while (true) {
                var res = next(ctx);
                if (res != Runners.NO_RETURN) return res;
            }
        }
        finally {
            end(ctx);
        }
    }

    public CodeFrame(Object thisArg, Object[] args, CodeFunction func) {
        this.args = args.clone();
        this.scope = new LocalScope(func.localsN, func.captures);
        this.scope.get(0).set(null, thisArg);
        var argsObj = new ArrayValue();
        for (var i = 0; i < args.length; i++) {
            argsObj.set(i, args[i]);
        }
        this.scope.get(1).value = argsObj;

        this.thisArg = thisArg;
        this.function = func;
    }
}
