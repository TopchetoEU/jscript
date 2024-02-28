package me.topchetoeu.jscript.core;

import me.topchetoeu.jscript.common.ResultRunnable;
import me.topchetoeu.jscript.common.events.DataNotifier;
import me.topchetoeu.jscript.core.exceptions.EngineException;

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
}
