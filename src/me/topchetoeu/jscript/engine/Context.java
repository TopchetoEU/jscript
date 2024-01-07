package me.topchetoeu.jscript.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.debug.DebugContext;
import me.topchetoeu.jscript.engine.frame.CodeFrame;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.lib.EnvironmentLib;
import me.topchetoeu.jscript.mapping.SourceMap;

public class Context implements Extensions {
    public static final Context NULL = new Context(null);

    public final Context parent;
    public final Environment environment;
    public final CodeFrame frame;
    public final Engine engine;
    public final int stackSize;

    @Override public <T> void add(Symbol key, T obj) {
        if (environment != null) environment.add(key, obj);
        else if (engine != null) engine.add(key, obj);
    }
    @Override public <T> T get(Symbol key) {
        if (environment != null && environment.has(key)) return environment.get(key);
        else if (engine != null && engine.has(key)) return engine.get(key);
        return null;
    }
    @Override public boolean has(Symbol key) {
        return
            environment != null && environment.has(key) ||
            engine != null && engine.has(key);
    }
    @Override public boolean remove(Symbol key) {
        var res = false;

        if (environment != null) res |= environment.remove(key);
        else if (engine != null) res |= engine.remove(key);

        return res;
    }
    @Override public Iterable<Symbol> keys() {
        if (engine == null && environment == null) return List.of();
        if (engine == null) return environment.keys();
        if (environment == null) return engine.keys();

        return () -> Stream.concat(
            StreamSupport.stream(engine.keys().spliterator(), false),
            StreamSupport.stream(environment.keys().spliterator(), false)
        ).distinct().iterator();
    }

    public FunctionValue compile(Filename filename, String raw) {
        var env = environment;
        var result = Environment.compileFunc(this).call(this, null, raw, filename.toString(), new EnvironmentLib(env));

        var function = (FunctionValue)Values.getMember(this, result, "function");
        if (!DebugContext.enabled(this)) return function;

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

    public Context pushFrame(CodeFrame frame) {
        var res = new Context(this, frame.function.environment, frame, engine, stackSize + 1);
        return res;
    }

    public Iterable<CodeFrame> frames() {
        var self = this;
        return () -> new Iterator<CodeFrame>() {
            private Context curr = self;

            private void update() {
                while (curr != null && curr.frame == null) curr = curr.parent;
            }

            @Override public boolean hasNext() {
                update();
                return curr != null;
            }
            @Override public CodeFrame next() {
                update();
                var res = curr.frame;
                curr = curr.parent;
                return res;
            }
        };
    }
    public List<String> stackTrace() {
        var res = new ArrayList<String>();

        for (var el : frames()) {
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

    private Context(Context parent, Environment environment, CodeFrame frame, Engine engine, int stackSize) {
        this.parent = parent;
        this.environment = environment;
        this.frame = frame;
        this.engine = engine;
        this.stackSize = stackSize;

        if (hasNotNull(Environment.MAX_STACK_COUNT) && stackSize > (int)get(Environment.MAX_STACK_COUNT)) {
            throw EngineException.ofRange("Stack overflow!");
        }
    }

    public Context(Engine engine) {
        this(null, null, null, engine, 0);
    }
    public Context(Engine engine, Environment env) {
        this(null, env, null, engine, 0);
    }
}
