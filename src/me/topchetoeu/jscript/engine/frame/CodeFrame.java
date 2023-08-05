package me.topchetoeu.jscript.engine.frame;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.BreakpointData;
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
    public static final DataKey<Integer> STACK_N_KEY = new DataKey<>();
    public static final DataKey<Integer> MAX_STACK_KEY = new DataKey<>();
    public static final DataKey<Boolean> STOP_AT_START_KEY = new DataKey<>();
    public static final DataKey<Boolean> STEPPING_TROUGH_KEY = new DataKey<>();

    public final LocalScope scope;
    public final Object thisArg;
    public final Object[] args;
    public final List<Object> stack = new ArrayList<>();
    public final CodeFunction function;

    public int codePtr = 0;
    private DebugCommand debugCmd = null;
    private Location prevLoc = null;

    public Object peek() {
        return peek(0);
    }
    public Object peek(int offset) {
        if (stack.size() <= offset) return null;
        else return stack.get(stack.size() - 1 - offset);
    }
    public Object pop() {
        if (stack.size() == 0) return null;
        else return stack.remove(stack.size() - 1);
    }
    public void push(Object val) {
        stack.add(stack.size(), Values.normalize(val));
    }

    private void cleanup(CallContext ctx) {
        stack.clear();
        codePtr = 0;
        debugCmd = null;
        var debugState = ctx.getData(Engine.DEBUG_STATE_KEY);

        if (debugState != null) debugState.popFrame();
        ctx.changeData(STACK_N_KEY, -1);
    }

    public Object next(CallContext ctx) throws InterruptedException {
        var debugState = ctx.getData(Engine.DEBUG_STATE_KEY);

        if (debugCmd == null) {
            if (ctx.getData(STACK_N_KEY, 0) >= ctx.addData(MAX_STACK_KEY, 1000))
                throw EngineException.ofRange("Stack overflow!");
            ctx.changeData(STACK_N_KEY);

            if (ctx.getData(STOP_AT_START_KEY, false)) debugCmd = DebugCommand.STEP_OVER;
            else debugCmd = DebugCommand.NORMAL;

            if (debugState != null) debugState.pushFrame(this);
        }

        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        var instr = function.body[codePtr];
        var loc = instr.location;
        if (loc != null) prevLoc = loc;

        if (debugState != null && loc != null) {
            if (
                instr.type == Type.NOP && instr.match("debug") || debugState.breakpoints.contains(loc) || (
                    ctx.getData(STEPPING_TROUGH_KEY, false) &&
                    (debugCmd == DebugCommand.STEP_INTO || debugCmd == DebugCommand.STEP_OVER)
                )
            ) {
                ctx.setData(STEPPING_TROUGH_KEY, true);

                debugState.breakpointNotifier.next(new BreakpointData(loc, ctx));
                debugCmd = debugState.commandNotifier.toAwaitable().await();
                if (debugCmd == DebugCommand.NORMAL) ctx.setData(STEPPING_TROUGH_KEY, false);
            }
        }

        try {
            var res = Runners.exec(debugCmd, instr, this, ctx);
            if (res != Runners.NO_RETURN) cleanup(ctx);
            return res;
        }
        catch (EngineException e) {
            cleanup(ctx);
            throw e.add(function.name, prevLoc);
        }
        catch (RuntimeException e) {
            cleanup(ctx);
            throw e;
        }
    }

    public Object run(CallContext ctx) throws InterruptedException {
        var debugState = ctx.getData(Engine.DEBUG_STATE_KEY);
        DebugCommand command = ctx.getData(STOP_AT_START_KEY, false) ? DebugCommand.STEP_OVER : DebugCommand.NORMAL;

        if (ctx.getData(STACK_N_KEY, 0) >= ctx.addData(MAX_STACK_KEY, 200)) throw EngineException.ofRange("Stack overflow!");
        ctx.changeData(STACK_N_KEY);

        if (debugState != null) debugState.pushFrame(this);

        Location loc = null;

        try {
            while (codePtr >= 0 && codePtr < function.body.length) {
                var _loc = function.body[codePtr].location;
                if (_loc != null) loc = _loc;

                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                var instr = function.body[codePtr];

                if (debugState != null && loc != null) {
                    if (
                        instr.type == Type.NOP && instr.match("debug") ||
                        (
                            (command == DebugCommand.STEP_INTO || command == DebugCommand.STEP_OVER) &&
                            ctx.getData(STEPPING_TROUGH_KEY, false)
                        ) ||
                        debugState.breakpoints.contains(loc)
                    ) {
                        ctx.setData(STEPPING_TROUGH_KEY, true);

                        debugState.breakpointNotifier.next(new BreakpointData(loc, ctx));
                        command = debugState.commandNotifier.toAwaitable().await();
                        if (command == DebugCommand.NORMAL) ctx.setData(STEPPING_TROUGH_KEY, false);
                    }
                }

                try {
                    var res = Runners.exec(command, instr, this, ctx);
                    if (res != Runners.NO_RETURN) return res;
                }
                catch (EngineException e) {
                    throw e.add(function.name, instr.location);
                }
            }
            return null;
        }
        // catch (StackOverflowError e) {
        //     e.printStackTrace();
        //     throw EngineException.ofRange("Stack overflow!").add(function.name, loc);
        // }
        finally {
            ctx.changeData(STACK_N_KEY, -1);
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
