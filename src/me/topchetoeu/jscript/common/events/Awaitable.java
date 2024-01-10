package me.topchetoeu.jscript.common.events;

public interface Awaitable<T> {
    public static interface ResultHandler<T> {
        public void onResult(T data);
    }
    public static interface ErrorHandler {
        public void onError(RuntimeException error);
    }

    T await();

    default void handle(ResultHandler<T> onResult, ErrorHandler onError) {
        var thread = new Thread(() -> {
            try {
                onResult.onResult(await());
            }
            catch (RuntimeException e) {
                onError.onError(e);
            }
        }, "Awaiter");
        thread.start();
    }
    default void handle(ResultHandler<T> onResult) {
        handle(onResult, err -> {});
    }
}
