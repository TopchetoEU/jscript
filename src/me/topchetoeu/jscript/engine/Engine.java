package me.topchetoeu.jscript.engine;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.PriorityBlockingQueue;

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
import me.topchetoeu.jscript.mapping.SourceMap;

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

    private static class Task implements Comparable<Task> {
        public final FunctionValue func;
        public final Object thisArg;
        public final Object[] args;
        public final DataNotifier<Object> notifier = new DataNotifier<>();
        public final Context ctx;
        public final boolean micro;

        public Task(Context ctx, FunctionValue func, Object thisArg, Object[] args, boolean micro) {
            this.ctx = ctx;
            this.func = func;
            this.thisArg = thisArg;
            this.args = args;
            this.micro = micro;
        }

        @Override
        public int compareTo(Task other) {
            return Integer.compare(this.micro ? 0 : 1, other.micro ? 0 : 1);
        }
    }

    private static int nextId = 0;
    public static final HashMap<Long, FunctionBody> functions = new HashMap<>();

    public final int id = ++nextId;
    public final boolean debugging;
    public int maxStackFrames = 10000;

    private final HashMap<Filename, String> sources = new HashMap<>();
    private final HashMap<Filename, TreeSet<Location>> bpts = new HashMap<>();
    private final HashMap<Filename, SourceMap> maps = new HashMap<>();

    public Location mapToCompiled(Location location) {
        var map = maps.get(location.filename());
        if (map == null) return location;
        return map.toCompiled(location);
    }
    public Location mapToOriginal(Location location) {
        var map = maps.get(location.filename());
        if (map == null) return location;
        return map.toOriginal(location);
    }

    private DebugController debugger;
    private Thread thread;
    private PriorityBlockingQueue<Task> tasks = new PriorityBlockingQueue<>();

    public synchronized boolean attachDebugger(DebugController debugger) {
        if (!debugging || this.debugger != null) return false;

        for (var source : sources.entrySet()) debugger.onSource(
            source.getKey(), source.getValue(),
            bpts.get(source.getKey()),
            maps.get(source.getKey())
        );

        this.debugger = debugger;
        return true;
    }
    public synchronized boolean detachDebugger() {
        if (!debugging || this.debugger == null) return false;
        this.debugger = null;
        return true;
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
        while (!untilEmpty || !tasks.isEmpty()) {
            try {
                runTask(tasks.take());
            }
            catch (InterruptedException | InterruptException e) {
                for (var msg : tasks) msg.notifier.error(new InterruptException(e));
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
    public synchronized boolean isRunning() {
        return this.thread != null;
    }

    public Awaitable<Object> pushMsg(boolean micro, Context ctx, FunctionValue func, Object thisArg, Object ...args) {
        var msg = new Task(ctx == null ? new Context(this) : ctx, func, thisArg, args, micro);
        tasks.add(msg);
        return msg.notifier;
    }
    public Awaitable<Object> pushMsg(boolean micro, Context ctx, Filename filename, String raw, Object thisArg, Object ...args) {
        return pushMsg(micro, ctx, new UncompiledFunction(filename, raw), thisArg, args);
    }

    @Override
    public void onFramePush(Context ctx, CodeFrame frame) {
        if (debugging && debugger != null) debugger.onFramePush(ctx, frame);
    }
    @Override public void onFramePop(Context ctx, CodeFrame frame) {
        if (debugging && debugger != null) debugger.onFramePop(ctx, frame);
    }
    @Override public boolean onInstruction(Context ctx, CodeFrame frame, Instruction instruction, Object returnVal, EngineException error, boolean caught) {
        if (debugging && debugger != null) return debugger.onInstruction(ctx, frame, instruction, returnVal, error, caught);
        else return false;
    }
    @Override public void onSource(Filename filename, String source, TreeSet<Location> breakpoints, SourceMap map) {
        if (!debugging) return;
        if (debugger != null) debugger.onSource(filename, source, breakpoints, map);
        sources.put(filename, source);
        bpts.put(filename, breakpoints);
        maps.put(filename, map);
    }

    public Engine(boolean debugging) {
        this.debugging = debugging;
    }
}
