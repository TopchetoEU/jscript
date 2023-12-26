package me.topchetoeu.jscript.events;

public interface Observable<T> {
    Handle on(Observer<T> val);

    default Handle once(Observer<T> observer) {
        // Java is fucking retarded
        var unhandler = new Handle[1];
        var shouldUnsub = new boolean[1];

        unhandler[0] = on(new Observer<>() {
            public void next(T data) {
                observer.next(data);
                if (unhandler[0] == null) shouldUnsub[0] = true;
                else unhandler[0].free();
            }
            public void error(RuntimeException err) {
                observer.error(err);
                if (unhandler[0] == null) shouldUnsub[0] = true;
                else unhandler[0].free();
            }
            public void finish() {
                observer.finish();
                if (unhandler[0] == null) shouldUnsub[0] = true;
                else unhandler[0].free();
            }
        });

        if (shouldUnsub[0]) {
            unhandler[0].free();
            return () -> {};
        }
        else return unhandler[0];
    }
    @SuppressWarnings("unchecked")
    default Awaitable<T> toAwaitable() {
        return () -> {
            var notifier = new Notifier();
            var valRef = new Object[1];
            var isErrRef = new boolean[1];

            once(new Observer<>() {
                public void next(T data) {
                    valRef[0] = data;
                    notifier.next();
                }
                public void error(RuntimeException err) {
                    isErrRef[0] = true;
                    valRef[0] = err;
                    notifier.next();
                }
                public void finish() {
                    isErrRef[0] = true;
                    valRef[0] = new FinishedException();
                    notifier.next();
                }
            });

            notifier.await();

            if (isErrRef[0]) throw (RuntimeException)valRef[0];
            else return (T)valRef[0];
        };
    }
    default Observable<T> encapsulate() {
        return val -> on(val);
    }

    default <T2> Observable<T2> pipe(Pipe<T, T2> pipe) {
        return sub -> on(pipe.apply(sub));
    }
    default WarmObservable<T> warmUp() {
        return new WarmObservable<>(this);
    }
}
