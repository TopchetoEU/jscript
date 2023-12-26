package me.topchetoeu.jscript.events;

import java.util.HashSet;

public class WarmObservable<T> implements Observable<T>, Handle {
    private HashSet<Observer<T>> observers = new HashSet<>();
    private Handle handle;

    @Override
    public Handle on(Observer<T> val) {
        if (observers == null) return () -> {};
        observers.add(val);
        return () -> observers.remove(val);
    }

    @Override
    public void free() {
        if (observers == null) return;
        handle.free();
        handle = null;
        observers = null;
    }

    public WarmObservable(Observable<T> observable) {
        observable.on(new Observer<>() {
            public void next(T data) {
                for (var obs : observers) obs.next(data);
            }
            public void error(RuntimeException err) {
                for (var obs : observers) obs.error(err);
                handle = null;
                observers = null;
            }
            public void finish() {
                for (var obs : observers) obs.finish();
                handle = null;
                observers = null;
            }
        });
    }

    @Override
    public WarmObservable<T> warmUp() {
        return this;
    }
}
