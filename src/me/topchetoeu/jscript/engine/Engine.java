package me.topchetoeu.jscript.engine;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingDeque;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.FunctionBody;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.engine.debug.DebugController;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.events.Awaitable;
import me.topchetoeu.jscript.events.DataNotifier;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.exceptions.InterruptException;

public class Engine implements DebugController {
    private class UncompiledFunction extends FunctionValue {
        public final Filename filename;
        public final String raw;
        private FunctionValue compiled = null;

        @Override
        public Object call(Context ctx, Object thisArg, Object ...args) {
            if (compiled == null) compiled = ctx.compile(filename, raw);
            return compiled.call(ctx, thisArg, args);
        }

        public UncompiledFunction(Filename filename, String raw) {
            super(filename + "", 0);
            this.filename = filename;
            this.raw = raw;
        }
    }

    private static class Task {
        public final FunctionValue func;
        public final Object thisArg;
        public final Object[] args;
        public final DataNotifier<Object> notifier = new DataNotifier<>();
        public final Context ctx;

        public Task(Context ctx, FunctionValue func, Object thisArg, Object[] args) {
            this.ctx = ctx;
            this.func = func;
            this.thisArg = thisArg;
            this.args = args;
        }
    }

    private static int nextId = 0;
    public static final HashMap<Long, FunctionBody> functions = new HashMap<>();

    public final int id = ++nextId;
    public final boolean debugging;
    public int maxStackFrames = 10000;

    private final HashMap<Filename, String> sources = new HashMap<>();
    private final HashMap<Filename, TreeSet<Location>> bpts = new HashMap<>();

    private DebugController debugger;
    private Thread thread;
    private LinkedBlockingDeque<Task> macroTasks = new LinkedBlockingDeque<>();
    private LinkedBlockingDeque<Task> microTasks = new LinkedBlockingDeque<>();

    public boolean attachDebugger(DebugController debugger) {
        if (!debugging || this.debugger != null) return false;

        for (var source : sources.entrySet()) {
            debugger.onSource(source.getKey(), source.getValue(), bpts.get(source.getKey()));
        }

        this.debugger = debugger;
        return true;
    }
    public boolean detachDebugger() {
        if (!debugging || this.debugger == null) return false;
        this.debugger = null;
        return true;
    }

    @Override public void onFramePop(Context ctx, CodeFrame frame) {
        if (debugging && debugger != null) debugger.onFramePop(ctx, frame);
    }
    @Override public boolean onInstruction(Context ctx, CodeFrame frame, Instruction instruction, Object returnVal, EngineException error, boolean caught) {
        if (debugging && debugger != null) return debugger.onInstruction(ctx, frame, instruction, returnVal, error, caught);
        else return false;
    }
    @Override public void onSource(Filename filename, String source, TreeSet<Location> breakpoints) {
        if (!debugging) return;
        if (debugger != null) debugger.onSource(filename, source, breakpoints);
        sources.put(filename, source);
        bpts.put(filename, breakpoints);
    }

    private void runTask(Task task) {
        try {
            task.notifier.next(task.func.call(task.ctx, task.thisArg, task.args));
        }
        catch (RuntimeException e) {
            if (e instanceof InterruptException) throw e;
            task.notifier.error(e);
        }
    }
    public void run(boolean untilEmpty) {
        while (!untilEmpty || !macroTasks.isEmpty()) {
            try {
                runTask(macroTasks.take());

                while (!microTasks.isEmpty()) {
                    runTask(microTasks.take());
                }
            }
            catch (InterruptedException | InterruptException e) {
                for (var msg : macroTasks) {
                    msg.notifier.error(new InterruptException(e));
                }
                break;
            }
        }
    }

    public Thread start() {
        if (this.thread == null) {
            this.thread = new Thread(() -> run(false), "JavaScript Runner #" + id);
            this.thread.start();
        }
        return this.thread;
    }
    public void stop() {
        thread.interrupt();
        thread = null;
    }
    public boolean inExecThread() {
        return Thread.currentThread() == thread;
    }
    public boolean isRunning() {
        return this.thread != null;
    }

    public Awaitable<Object> pushMsg(boolean micro, Context ctx, FunctionValue func, Object thisArg, Object ...args) {
        var msg = new Task(ctx == null ? new Context(this) : ctx, func, thisArg, args);
        if (micro) microTasks.addLast(msg);
        else macroTasks.addLast(msg);
        return msg.notifier;
    }
    public Awaitable<Object> pushMsg(boolean micro, Context ctx, Filename filename, String raw, Object thisArg, Object ...args) {
        return pushMsg(micro, ctx, new UncompiledFunction(filename, raw), thisArg, args);
    }

    public Engine(boolean debugging) {
        this.debugging = debugging;
    }
}
