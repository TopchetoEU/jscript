package me.topchetoeu.jscript.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.TreeSet;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.parsing.Parsing;

public class Context {
    private final Stack<Environment> env = new Stack<>();
    private final ArrayList<CodeFrame> frames = new ArrayList<>();
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

        var breakpoints = new TreeSet<Location>();
        FunctionValue res = Parsing.compile(Engine.functions, breakpoints, environment(), filename, source);
        engine.onSource(filename, source, breakpoints);

        if (runner != null) res = (FunctionValue)runner.call(this, null, res);

        return res;
    }


    public void pushFrame(CodeFrame frame) {
        frames.add(frame);
        if (frames.size() > engine.maxStackFrames) throw EngineException.ofRange("Stack overflow!");
        pushEnv(frame.function.environment);
    }
    public boolean popFrame(CodeFrame frame) {
        if (frames.size() == 0) return false;
        if (frames.get(frames.size() - 1) != frame) return false;
        frames.remove(frames.size() - 1);
        popEnv();
        engine.onFramePop(this, frame);
        return true;
    }
    public CodeFrame peekFrame() {
        if (frames.size() == 0) return null;
        return frames.get(frames.size() - 1);
    }

    public List<CodeFrame> frames() {
        return Collections.unmodifiableList(frames);
    }
    public List<String> stackTrace() {
        var res = new ArrayList<String>();

        for (var i = frames.size() - 1; i >= 0; i--) {
            var el = frames.get(i);
            var name = el.function.name;
            Location loc = null;

            for (var j = el.codePtr; j >= 0 && loc == null; j--) loc = el.function.body[j].location;
            if (loc == null) loc = el.function.loc();

            var trace = "";

            if (loc != null) trace += "at " + loc.toString() + " ";
            if (name != null && !name.equals("")) trace += "in " + name + " ";

            trace = trace.trim();

            if (!trace.equals("")) res.add(trace);
        }

        return res;
    }

    public Context(Engine engine) {
        this.data = new Data();
        this.engine = engine;
    }
    public Context(Engine engine, Environment env) {
        this(engine);
        this.pushEnv(env);
        
    }
}
