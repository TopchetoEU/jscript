package me.topchetoeu.jscript.engine;

import java.util.concurrent.LinkedBlockingDeque;

import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.events.Awaitable;
import me.topchetoeu.jscript.events.DataNotifier;
import me.topchetoeu.jscript.exceptions.EngineException;

public class Engine {
    private class UncompiledFunction extends FunctionValue {
        public final String filename;
        public final String raw;
        public final Environment ctx;

        @Override
        public Object call(Context ctx, Object thisArg, Object ...args) throws InterruptedException {
            ctx = new Context(this.ctx, ctx.message);
            return ctx.compile(filename, raw).call(ctx, thisArg, args);
        }

        public UncompiledFunction(Environment ctx, String filename, String raw) {
            super(filename, 0);
            this.filename = filename;
            this.raw = raw;
            this.ctx = ctx; 
        }
    }

    private static class Task {
        public final FunctionValue func;
        public final Object thisArg;
        public final Object[] args;
        public final DataNotifier<Object> notifier = new DataNotifier<>();
        public final Message ctx;

        public Task(Message ctx, FunctionValue func, Object thisArg, Object[] args) {
            this.ctx = ctx;
            this.func = func;
            this.thisArg = thisArg;
            this.args = args;
        }
    }

    private static int nextId = 0;

    private Thread thread;
    private LinkedBlockingDeque<Task> macroTasks = new LinkedBlockingDeque<>();
    private LinkedBlockingDeque<Task> microTasks = new LinkedBlockingDeque<>();

    public final int id = ++nextId;

    private void runTask(Task task) throws InterruptedException {
        try {
            task.notifier.next(task.func.call(new Context(null, task.ctx), task.thisArg, task.args));
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

    public Awaitable<Object> pushMsg(boolean micro, Message ctx, FunctionValue func, Object thisArg, Object ...args) {
        var msg = new Task(ctx, func, thisArg, args);
        if (micro) microTasks.addLast(msg);
        else macroTasks.addLast(msg);
        return msg.notifier;
    }
    public Awaitable<Object> pushMsg(boolean micro, Context ctx, String filename, String raw, Object thisArg, Object ...args) {
        return pushMsg(micro, ctx.message, new UncompiledFunction(ctx.env, filename, raw), thisArg, args);
    }

    // public Engine() {
    //     this.typeRegister = new NativeTypeRegister();
    // }
}
