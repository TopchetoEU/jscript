package me.topchetoeu.jscript.core;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.core.debug.DebugContext;
import me.topchetoeu.jscript.core.values.FunctionValue;
import me.topchetoeu.jscript.core.values.Symbol;
import me.topchetoeu.jscript.core.exceptions.EngineException;
import me.topchetoeu.jscript.lib.EnvironmentLib;

public class Context implements Extensions {
    public static final Context NULL = new Context(null);

    public final Context parent;
    public final Environment environment;
    public final Frame frame;
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

        DebugContext.get(this).onSource(filename, raw);
        return (FunctionValue)result;

        // var rawMapChain = ((ArrayValue)Values.getMember(this, result, "mapChain")).toArray();
        // var breakpoints = new TreeSet<>(
        //     Arrays.stream(((ArrayValue)Values.getMember(this, result, "breakpoints")).toArray())
        //         .map(v -> Location.parse(Values.toString(this, v)))
        //         .collect(Collectors.toList())
        // );
        // var maps = new SourceMap[rawMapChain.length];

        // for (var i = 0; i < maps.length; i++) maps[i] = SourceMap.parse(Values.toString(this, (String)rawMapChain[i]));

        // var map = SourceMap.chain(maps);

        // if (map != null) {
        //     var newBreakpoints = new TreeSet<Location>();
        //     for (var bp : breakpoints) {
        //         bp = map.toCompiled(bp);
        //         if (bp != null) newBreakpoints.add(bp);
        //     }
        //     breakpoints = newBreakpoints;
        // }
    }

    public Context pushFrame(Frame frame) {
        var res = new Context(this, frame.function.environment, frame, engine, stackSize + 1);
        return res;
    }

    public Iterable<Frame> frames() {
        var self = this;
        return () -> new Iterator<Frame>() {
            private Context curr = self;

            private void update() {
                while (curr != null && curr.frame == null) curr = curr.parent;
            }

            @Override public boolean hasNext() {
                update();
                return curr != null;
            }
            @Override public Frame next() {
                update();
                var res = curr.frame;
                curr = curr.parent;
                return res;
            }
        };
    }


    private Context(Context parent, Environment environment, Frame frame, Engine engine, int stackSize) {
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
