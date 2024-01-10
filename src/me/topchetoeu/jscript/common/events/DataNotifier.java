package me.topchetoeu.jscript.common.events;

public class DataNotifier<T> implements Awaitable<T> {
    private Notifier notifier = new Notifier();
    private boolean isErr;
    private T val;
    private RuntimeException err;

    public void error(RuntimeException t) {
        err = t;
        isErr = true;
        notifier.next();
    }
    public void next(T val) {
        this.val = val;
        isErr = false;
        notifier.next();
    }
    public T await() {
        notifier.await();

        try {
            if (isErr) throw err;
            else return val;
        }
        finally {
            this.err = null;
            this.val = null;
        }
    }
}
