package me.topchetoeu.jscript.engine;

import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import me.topchetoeu.jscript.engine.CallContext.DataKey;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.events.Awaitable;
import me.topchetoeu.jscript.events.DataNotifier;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.parsing.Parsing;

public class Engine {
    private class UncompiledFunction extends FunctionValue {
        public final String filename;
        public final String raw;

        @Override
        public Object call(CallContext ctx, Object thisArg, Object... args) throws InterruptedException {
            return compile(ctx, filename, raw).call(ctx, thisArg, args);
        }

        public UncompiledFunction(String filename, String raw) {
            super(filename, 0);
            this.filename = filename;
            this.raw = raw;
        }
    }

    private static class Task {
        public final FunctionValue func;
        public final Object thisArg;
        public final Object[] args;
        public final Map<DataKey<?>, Object> data;
        public final DataNotifier<Object> notifier = new DataNotifier<>();
        public final Environment env;

        public Task(Environment env, FunctionValue func, Map<DataKey<?>, Object> data, Object thisArg, Object[] args) {
            this.env = env;
            this.func = func;
            this.data = data;
            this.thisArg = thisArg;
            this.args = args;
        }
    }

    private static int nextId = 0;

    // private Map<DataKey<?>, Object> callCtxVals = new HashMap<>();
    // private NativeTypeRegister typeRegister;
    private Thread thread;

    private LinkedBlockingDeque<Task> macroTasks = new LinkedBlockingDeque<>();
    private LinkedBlockingDeque<Task> microTasks = new LinkedBlockingDeque<>();

    public final int id = ++nextId;

    // public NativeTypeRegister typeRegister() { return typeRegister; }

    private void runTask(Task task) throws InterruptedException {
        try {
            task.notifier.next(task.func.call(new CallContext(this, task.env).mergeData(task.data), task.thisArg, task.args));
        }
        catch (InterruptedException e) {
            task.notifier.error(new RuntimeException(e));
            throw e;
        }
        catch (EngineException e) {
            task.notifier.error(e);
        }
        catch (RuntimeException e) {
            task.notifier.error(e);
            e.printStackTrace();
        }
    }
    private void run() {
        while (true) {
            try {
                runTask(macroTasks.take());

                while (!microTasks.isEmpty()) {
                    runTask(microTasks.take());
                }
            }
            catch (InterruptedException e) {
                for (var msg : macroTasks) {
                    msg.notifier.error(new RuntimeException(e));
                }
                break;
            }
        }
    }

    public Thread start() {
        if (this.thread == null) {
            this.thread = new Thread(this::run, "JavaScript Runner #" + id);
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

    public Awaitable<Object> pushMsg(boolean micro, Map<DataKey<?>, Object> data, Environment env, FunctionValue func, Object thisArg, Object... args) {
        var msg = new Task(env, func, data, thisArg, args);
        if (micro) microTasks.addLast(msg);
        else macroTasks.addLast(msg);
        return msg.notifier;
    }
    public Awaitable<Object> pushMsg(boolean micro, Map<DataKey<?>, Object> data, Environment env, String filename, String raw, Object thisArg, Object... args) {
        return pushMsg(micro, data, env, new UncompiledFunction(filename, raw), thisArg, args);
    }

    public FunctionValue compile(CallContext ctx, String filename, String raw) throws InterruptedException {
        var res = Values.toString(ctx, ctx.environment.compile.call(ctx, null, raw, filename));
        return Parsing.compile(ctx.environment, filename, res);
    }

    // public Engine() {
    //     this.typeRegister = new NativeTypeRegister();
    // }
}
