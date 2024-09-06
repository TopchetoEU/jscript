package me.topchetoeu.jscript.runtime;

import java.util.concurrent.Future;
import java.util.function.Supplier;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.common.parsing.Filename;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;

public interface EventLoop {
    public static final Key<EventLoop> KEY = Key.of();

    public static EventLoop get(Environment ext) {
        if (ext.hasNotNull(KEY)) return ext.get(KEY);
        else return new EventLoop() {
            @Override public <T> Future<T> pushMsg(Supplier<T> runnable, boolean micro) {
                throw EngineException.ofError("No event loop attached to environment.");
            }
        };
    }

    public <T> Future<T> pushMsg(Supplier<T> runnable, boolean micro);
    public default Future<Void> pushMsg(Runnable runnable, boolean micro) {
        return pushMsg(() -> { runnable.run(); return null; }, micro);
    }

    public default Future<Value> pushMsg(boolean micro, Environment env, FunctionValue func, Value thisArg, Value ...args) {
        return pushMsg(() -> func.invoke(env, thisArg, args), micro);
    }
    public default Future<Value> pushMsg(boolean micro, Environment env, Filename filename, String raw, Value thisArg, Value ...args) {
        return pushMsg(() -> Compiler.compileFunc(env, filename, raw).invoke(env, thisArg, args), micro);
    }
}
