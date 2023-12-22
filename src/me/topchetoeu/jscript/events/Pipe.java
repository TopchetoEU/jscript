package me.topchetoeu.jscript.events;

public interface Pipe<T, T2> {
    Observer<T> apply(Observer<T2> obs);
    // void next(T val, Observer<T2> target);
    // default void error(RuntimeException err, Observer<T2> target) {
    //     target.error(err);
    // }
    // default void finish(Observer<T2> target) {
    //     target.finish();
    // }

    public static interface MapFunc<T1, T2> {
        T2 map(T1 val);
    }

    public static <T1, T2> Pipe<T1, T2> map(MapFunc<T1, T2> func) {
        return o -> val -> o.next(func.map(val));
    }
    public static <T> Pipe<T, T> filter(MapFunc<T, Boolean> func) {
        return o -> val -> {
            if (func.map(val)) o.next(val);
        };
    }
    public static <T> Pipe<T, T> skip(int n) {
        var i = new int[1];

        return target -> val -> {
            if (i[0] >= n) target.next(val);
            else i[0]++;
        };
    }
    public static <T> Pipe<T, T> limit(int n) {
        return target -> new Observer<T>() {
            private int i;

            public void next(T val) {
                if (i >= n) target.finish();
                else {
                    target.next(val);
                    i++;
                }
            }
            public void error(RuntimeException err) {
                if (i < n) target.error(err);
            }
            public void finish() {
                if (i < n) target.finish();
            }
        };
    }
    public static <T> Pipe<T, T> first() {
        return limit(1);
    }

    public static <T> Pipe<Observable<T>, T> merge() {
        return target -> val -> val.on(target);
    }
}