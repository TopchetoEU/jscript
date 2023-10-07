package me.topchetoeu.jscript.engine;

import java.util.Stack;

import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.parsing.Parsing;

public class Context {
    private final Stack<Environment> env = new Stack<>();
    public final Data data;
    public final Engine engine;

    public Environment environment() {
        return env.empty() ? null : env.peek();
    }

    public Context pushEnv(Environment env) {
        this.env.push(env);
        return this;
    }
    public void popEnv() {
        if (!env.empty()) this.env.pop();
    }

    public FunctionValue compile(String filename, String raw) throws InterruptedException {
        var res = Values.toString(this, environment().compile.call(this, null, raw, filename));
        return Parsing.compile(engine.functions, environment(), filename, res);
    }

    public Context(Engine engine, Data data) {
        this.data = new Data(engine.data);
        if (data != null) this.data.addAll(data);
        this.engine = engine;
    }
    public Context(Engine engine) {
        this(engine, null);
    }
}
