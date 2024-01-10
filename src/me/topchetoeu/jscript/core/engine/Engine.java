package me.topchetoeu.jscript.core.engine;

import java.util.HashMap;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.events.Awaitable;
import me.topchetoeu.jscript.core.compilation.FunctionBody;
import me.topchetoeu.jscript.core.engine.values.FunctionValue;
import me.topchetoeu.jscript.core.engine.values.Symbol;

public class Engine extends EventLoop implements Extensions {
    public static final HashMap<Long, FunctionBody> functions = new HashMap<>();

    private final Environment env = new Environment();

    @Override
    public <T> void add(Symbol key, T obj) {
        this.env.add(key, obj);
    }
    @Override
    public <T> T get(Symbol key) {
        return this.env.get(key);
    }
    @Override
    public boolean has(Symbol key) {
        return this.env.has(key);
    }
    @Override
    public boolean remove(Symbol key) {
        return this.env.remove(key);
    }
    @Override
    public Iterable<Symbol> keys() {
        return env.keys();
    }

    public Engine copy() {
        var res = new Engine();
        res.env.addAll(env);
        return res;
    }

    public Awaitable<Object> pushMsg(boolean micro, Environment env, FunctionValue func, Object thisArg, Object ...args) {
        return pushMsg(() -> {
            return func.call(new Context(this, env), thisArg, args);
        }, micro);
    }
    public Awaitable<Object> pushMsg(boolean micro, Environment env, Filename filename, String raw, Object thisArg, Object ...args) {
        return pushMsg(() -> {
            var ctx = new Context(this, env);
            return ctx.compile(filename, raw).call(new Context(this, env), thisArg, args);
        }, micro);
    }

    public Engine() {
    }
}
