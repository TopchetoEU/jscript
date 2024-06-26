package me.topchetoeu.jscript.runtime;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.ResultRunnable;
import me.topchetoeu.jscript.common.events.DataNotifier;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.FunctionValue;

public interface EventLoop {
    public static final Key<EventLoop> KEY = new Key<>();

    public static EventLoop get(Extensions ext) {
        if (ext.hasNotNull(KEY)) return ext.get(KEY);
        else return new EventLoop() {
            @Override public <T> DataNotifier<T> pushMsg(ResultRunnable<T> runnable, boolean micro) {
                throw EngineException.ofError("No event loop attached to environment.");
            }
        };
    }

    public <T> DataNotifier<T> pushMsg(ResultRunnable<T> runnable, boolean micro);
    public default DataNotifier<Void> pushMsg(Runnable runnable, boolean micro) {
        return pushMsg(() -> { runnable.run(); return null; }, micro);
    }

    public default DataNotifier<Object> pushMsg(boolean micro, Extensions ext, FunctionValue func, Object thisArg, Object ...args) {
        return pushMsg(() -> {
            return func.call(Context.of(ext), thisArg, args);
        }, micro);
    }
    public default DataNotifier<Object> pushMsg(boolean micro, Extensions ext, Filename filename, String raw, Object thisArg, Object ...args) {
        return pushMsg(() -> {
            var ctx = Context.of(ext);
            return ctx.compile(filename, raw).call(Context.of(ext), thisArg, args);
        }, micro);
    }
}
