package me.topchetoeu.jscript.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Supplier;

import me.topchetoeu.jscript.runtime.exceptions.InterruptException;

public final class Engine implements EventLoop {
    private static class Task<T> implements Comparable<Task<?>> {
        public final Supplier<?> runnable;
        public final CompletableFuture<T> notifier = new CompletableFuture<T>();
        public final boolean micro;

        public Task(Supplier<T> runnable, boolean micro) {
            this.runnable = runnable;
            this.micro = micro;
        }

        @Override public int compareTo(Task<?> other) {
            return Integer.compare(this.micro ? 0 : 1, other.micro ? 0 : 1);
        }
    }

    private PriorityBlockingQueue<Task<?>> tasks = new PriorityBlockingQueue<>();
    private Thread thread;

    @Override public <T> Future<T> pushMsg(Supplier<T> runnable, boolean micro) {
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
                    ((Task<Object>)task).notifier.complete(task.runnable.get());
                }
                catch (RuntimeException e) {
                    if (e instanceof InterruptException) throw e;
                    task.notifier.completeExceptionally(e);
                }
            }
            catch (InterruptedException | InterruptException e) {
                for (var msg : tasks) msg.notifier.cancel(false);
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
