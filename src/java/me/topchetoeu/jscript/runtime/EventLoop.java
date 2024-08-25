package me.topchetoeu.jscript.runtime;

import me.topchetoeu.jscript.common.Compiler;
import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.ResultRunnable;
import me.topchetoeu.jscript.common.events.DataNotifier;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.environment.Key;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;

public interface EventLoop {
    public static final Key<EventLoop> KEY = new Key<>();

    public static EventLoop get(Environment ext) {
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

    public default DataNotifier<Value> pushMsg(boolean micro, Environment env, FunctionValue func, Value thisArg, Value ...args) {
        return pushMsg(() -> func.call(env, thisArg, args), micro);
    }
    public default DataNotifier<Value> pushMsg(boolean micro, Environment env, Filename filename, String raw, Value thisArg, Value ...args) {
        return pushMsg(() -> Compiler.compile(env, filename, raw).call(env, thisArg, args), micro);
    }
}
