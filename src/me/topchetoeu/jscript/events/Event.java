package me.topchetoeu.jscript.events;

import java.util.HashSet;

public class Event<T> implements Observer<T>, Observable<T> {
    private HashSet<Observer<T>> handlers = new HashSet<>();

    public Handle on(Observer<T> handler) {
        if (handlers == null) {
            handler.finish();
            return () -> {};
        }

        handlers.add(handler);
        return () -> {
            if (handlers == null) return;
            handlers.remove(handler);
        };
    }

    public boolean isFinished() {
        return handlers == null;
    }

    public void next(T value) {
        if (handlers == null) throw new IllegalStateException("Cannot use a finished event.");
        for (var handler : handlers) {
            handler.next(value);
        }
    }
    public void error(RuntimeException value) {
        if (handlers == null) throw new IllegalStateException("Cannot use a finished event.");
        for (var handler : handlers) {
            handler.error(value);
        }

        handlers.clear();
        handlers = null;
    }
    public void finish() {
        if (handlers == null) throw new IllegalStateException("Cannot use a finished event.");
        for (var handler : handlers) {
            handler.finish();
        }

        handlers.clear();
        handlers = null;
    }
}
