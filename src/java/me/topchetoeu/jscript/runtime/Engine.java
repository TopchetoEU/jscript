package me.topchetoeu.jscript.runtime;

import java.util.concurrent.PriorityBlockingQueue;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.ResultRunnable;
import me.topchetoeu.jscript.common.events.DataNotifier;
import me.topchetoeu.jscript.runtime.exceptions.InterruptException;
import me.topchetoeu.jscript.runtime.values.FunctionValue;

public class Engine implements EventLoop {
    private static class Task<T> implements Comparable<Task<?>> {
        public final ResultRunnable<?> runnable;
        public final DataNotifier<T> notifier = new DataNotifier<>();
        public final boolean micro;

        public Task(ResultRunnable<T> runnable, boolean micro) {
            this.runnable = runnable;
            this.micro = micro;
        }

        @Override
        public int compareTo(Task<?> other) {
            return Integer.compare(this.micro ? 0 : 1, other.micro ? 0 : 1);
        }
    }

    private PriorityBlockingQueue<Task<?>> tasks = new PriorityBlockingQueue<>();
    private Thread thread;

    @Override
    public <T> DataNotifier<T> pushMsg(ResultRunnable<T> runnable, boolean micro) {
        var msg = new Task<T>(runnable, micro);
        tasks.add(msg);
        return msg.notifier;
    }

    @SuppressWarnings("unchecked")
    public void run(boolean untilEmpty) {
        while (!untilEmpty || !tasks.isEmpty()) {
            try {
                var task = tasks.take();

                try {
                    ((Task<Object>)task).notifier.next(task.runnable.run());
                }
                catch (RuntimeException e) {
                    if (e instanceof InterruptException) throw e;
                    task.notifier.error(e);
                }
            }
            catch (InterruptedException | InterruptException e) {
                for (var msg : tasks) msg.notifier.error(new InterruptException(e));
                break;
            }
        }
    }

    public Thread thread() {
        return thread;
    }
    public Thread start() {
        if (thread == null) {
            thread = new Thread(() -> run(false), "Event loop #" + hashCode());
            thread.start();
        }
        return thread;
    }
    public void stop() {
        if (thread != null) thread.interrupt();
        thread = null;
    }

    public boolean inLoopThread() {
        return Thread.currentThread() == thread;
    }
    public boolean isRunning() {
        return this.thread != null;
    }

    public DataNotifier<Object> pushMsg(boolean micro, Environment env, FunctionValue func, Object thisArg, Object ...args) {
        return pushMsg(() -> {
            return func.call(new Context(env), thisArg, args);
        }, micro);
    }
    public DataNotifier<Object> pushMsg(boolean micro, Environment env, Filename filename, String raw, Object thisArg, Object ...args) {
        return pushMsg(() -> {
            var ctx = new Context(env);
            return ctx.compile(filename, raw).call(new Context(env), thisArg, args);
        }, micro);
    }

    public Engine() {
    }
}
