package me.topchetoeu.jscript.events;

import me.topchetoeu.jscript.exceptions.InterruptException;

public interface Awaitable<T> {
    T await() throws FinishedException;

    default Observable<T> toObservable() {
        return sub -> {
            var thread = new Thread(() -> {
                try {
                    sub.next(await());
                    sub.finish();
                }
                catch (InterruptException | FinishedException e) { sub.finish(); }
                catch (RuntimeException e) {
                    sub.error(e);
                }
            }, "Awaiter");
            thread.start();

            return () -> thread.interrupt();
        };
    }
}
