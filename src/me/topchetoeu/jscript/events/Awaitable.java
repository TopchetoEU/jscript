package me.topchetoeu.jscript.events;

public interface Awaitable<T> {
    T await() throws FinishedException, InterruptedException;

    default Observable<T> toObservable() {
        return sub -> {
            var thread = new Thread(() -> {
                try {
                    sub.next(await());
                    sub.finish();
                }
                catch (InterruptedException | FinishedException e) {
                    sub.finish();
                }
                catch (RuntimeException e) {
                    sub.error(e);
                }
            }, "Awaiter");
            thread.start();

            return () -> thread.interrupt();
        };
    }
}
