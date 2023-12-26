package me.topchetoeu.jscript.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.TreeSet;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.debug.DebugContext;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.mapping.SourceMap;

public class Context extends ExtensionStack {
    private final Stack<Environment> env = new Stack<>();
    private final ArrayList<CodeFrame> frames = new ArrayList<>();
    public final Engine engine;

    @Override
    protected Extensions[] extensionStack() {
        return new Extensions[] { environment(), engine.globalEnvironment };
    }

    public Environment environment() {
        return env.empty() ? null : env.peek();
    }

    private Context pushEnv(Environment env) {
        this.env.push(env);
        return this;
    }
    private void popEnv() {
        if (!env.empty()) this.env.pop();
    }

    public FunctionValue compile(Filename filename, String raw) {
        var env = environment();
        var result = Environment.compileFunc(this).call(this, null, raw, filename.toString(), env);

        var function = (FunctionValue)Values.getMember(this, result, "function");
        if (!engine.debugging) return function;

        var rawMapChain = ((ArrayValue)Values.getMember(this, result, "mapChain")).toArray();
        var breakpoints = new TreeSet<>(
            Arrays.stream(((ArrayValue)Values.getMember(this, result, "breakpoints")).toArray())
                .map(v -> Location.parse(Values.toString(this, v)))
                .collect(Collectors.toList())
        );
        var maps = new SourceMap[rawMapChain.length];

        for (var i = 0; i < maps.length; i++) maps[i] = SourceMap.parse(Values.toString(this, (String)rawMapChain[i]));

        var map = SourceMap.chain(maps);

        if (map != null) {
            var newBreakpoints = new TreeSet<Location>();
            for (var bp : breakpoints) {
                bp = map.toCompiled(bp);
                if (bp != null) newBreakpoints.add(bp);
            }
            breakpoints = newBreakpoints;
        }

        DebugContext.get(this).onSource(filename, raw, breakpoints, map);

        return function;
    }

    public void pushFrame(CodeFrame frame) {
        frames.add(frame);
        if (frames.size() > engine.maxStackFrames) throw EngineException.ofRange("Stack overflow!");
        pushEnv(frame.function.environment);
        DebugContext.get(this).onFramePush(this, frame);
    }
    public boolean popFrame(CodeFrame frame) {
        if (frames.size() == 0) return false;
        if (frames.get(frames.size() - 1) != frame) return false;
        frames.remove(frames.size() - 1);
        popEnv();
        DebugContext.get(this).onFramePop(this, frame);
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
        this.engine = engine;
    }
    public Context(Engine engine, Environment env) {
        this(engine);
        if (env != null) this.pushEnv(env);
    }
}
