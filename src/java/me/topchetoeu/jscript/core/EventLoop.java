package me.topchetoeu.jscript.core;

import java.util.concurrent.PriorityBlockingQueue;

import me.topchetoeu.jscript.common.ResultRunnable;
import me.topchetoeu.jscript.common.events.DataNotifier;
import me.topchetoeu.jscript.core.exceptions.InterruptException;

public class EventLoop {
    private static class Task implements Comparable<Task> {
        public final ResultRunnable<?> runnable;
        public final DataNotifier<Object> notifier = new DataNotifier<>();
        public final boolean micro;

        public Task(ResultRunnable<?> runnable, boolean micro) {
            this.runnable = runnable;
            this.micro = micro;
        }

        @Override
        public int compareTo(Task other) {
            return Integer.compare(this.micro ? 0 : 1, other.micro ? 0 : 1);
        }
    }

    private PriorityBlockingQueue<Task> tasks = new PriorityBlockingQueue<>();
    private Thread thread;

    @SuppressWarnings("unchecked")
    public <T> DataNotifier<T> pushMsg(ResultRunnable<T> runnable, boolean micro) {
        var msg = new Task(runnable, micro);
        tasks.add(msg);
        return (DataNotifier<T>)msg.notifier;
    }
    public DataNotifier<Void> pushMsg(Runnable runnable, boolean micro) {
        return pushMsg(() -> { runnable.run(); return null; }, micro);
    }

    public void run(boolean untilEmpty) {
        while (!untilEmpty || !tasks.isEmpty()) {
            try {
                var task = tasks.take();

                try {
                    task.notifier.next(task.runnable.run());
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
}
