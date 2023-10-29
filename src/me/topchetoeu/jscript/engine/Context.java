package me.topchetoeu.jscript.engine;

import java.util.Stack;
import java.util.TreeSet;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
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

    public FunctionValue compile(Filename filename, String raw) {
        var transpiled = environment().compile.call(this, null, raw, filename.toString());
        String source = null;
        FunctionValue runner = null;

        if (transpiled instanceof ObjectValue) {
            source = Values.toString(this, Values.getMember(this, transpiled, "source"));
            var _runner = Values.getMember(this, transpiled, "runner");
            if (_runner instanceof FunctionValue) runner = (FunctionValue)_runner;
        }
        else source = Values.toString(this, transpiled);

        var debugger = StackData.getDebugger(this);
        var breakpoints = new TreeSet<Location>();
        FunctionValue res = Parsing.compile(engine.functions, breakpoints, environment(), filename, source);
        if (debugger != null) debugger.onSource(filename, source, breakpoints);

        if (runner != null) res = (FunctionValue)runner.call(this, null, res);

        return res;
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
