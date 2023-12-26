package me.topchetoeu.jscript.engine;

import java.util.HashMap;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.engine.scope.GlobalScope;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeWrapperProvider;
import me.topchetoeu.jscript.parsing.Parsing;

// TODO: Remove hardcoded extensions form environment
@SuppressWarnings("unchecked")
public class Environment implements Extensions {
    private static int nextId = 0;

    public static final HashMap<String, Symbol> symbols = new HashMap<>();

    public static final Symbol WRAPPERS = Symbol.get("Environment.wrappers");
    public static final Symbol COMPILE_FUNC = Symbol.get("Environment.compile");

    public static final Symbol REGEX_CONSTR = Symbol.get("Environment.regexConstructor");
    public static final Symbol STACK = Symbol.get("Environment.stack");
    public static final Symbol HIDE_STACK = Symbol.get("Environment.hideStack");

    public static final Symbol OBJECT_PROTO = Symbol.get("Environment.objectPrototype");
    public static final Symbol FUNCTION_PROTO = Symbol.get("Environment.functionPrototype");
    public static final Symbol ARRAY_PROTO = Symbol.get("Environment.arrayPrototype");
    public static final Symbol BOOL_PROTO = Symbol.get("Environment.boolPrototype");
    public static final Symbol NUMBER_PROTO = Symbol.get("Environment.numberPrototype");
    public static final Symbol STRING_PROTO = Symbol.get("Environment.stringPrototype");
    public static final Symbol SYMBOL_PROTO = Symbol.get("Environment.symbolPrototype");
    public static final Symbol ERROR_PROTO = Symbol.get("Environment.errorPrototype");
    public static final Symbol SYNTAX_ERR_PROTO = Symbol.get("Environment.syntaxErrorPrototype");
    public static final Symbol TYPE_ERR_PROTO = Symbol.get("Environment.typeErrorPrototype");
    public static final Symbol RANGE_ERR_PROTO = Symbol.get("Environment.rangeErrorPrototype");

    private HashMap<Symbol, Object> data = new HashMap<>();

    public GlobalScope global;
    public WrappersProvider wrappers;

    @Native public int id = ++nextId;

    @Override public <T> void add(Symbol key, T obj) {
        data.put(key, obj);
    }
    @Override public <T> T get(Symbol key) {
        return (T)data.get(key);
    }
    @Override public boolean remove(Symbol key) {
        if (data.containsKey(key)) {
            data.remove(key);
            return true;
        }
        return false;
    }
    @Override public boolean has(Symbol key) {
        return data.containsKey(key);
    }

    public static FunctionValue compileFunc(Extensions ext) {
        return ext.init(COMPILE_FUNC, new NativeFunction("compile", (ctx, thisArg, args) -> {
            var source = Values.toString(ctx, args[0]);
            var filename = Values.toString(ctx, args[1]);
            var isDebug = Values.toBoolean(args[2]);

            var env = Values.wrapper(args[2], Environment.class);
            var res = new ObjectValue();

            var target = Parsing.compile(env, Filename.parse(filename), source);
            Engine.functions.putAll(target.functions);
            Engine.functions.remove(0l);

            res.defineProperty(ctx, "function", target.func(env));
            res.defineProperty(ctx, "mapChain", new ArrayValue());

            if (isDebug) {
                res.defineProperty(ctx, "breakpoints", ArrayValue.of(ctx, target.breakpoints.stream().map(Location::toString).collect(Collectors.toList())));
            }

            return res;
        }));
    }
    public static FunctionValue regexConstructor(Extensions ext) {
        return ext.init(COMPILE_FUNC, new NativeFunction("RegExp", (ctx, thisArg, args) -> {
            throw EngineException.ofError("Regular expressions not supported.").setCtx(ctx.environment(), ctx.engine);
        }));
    }

    public Environment fork() {
        var res = new Environment(null, global);

        res.wrappers = wrappers.fork(res);
        res.global = global;
        res.data.putAll(data);

        return res;
    }
    public Environment child() {
        var res = fork();
        res.global = res.global.globalChild();
        return res;
    }

    public Context context(Engine engine) {
        return new Context(engine, this);
    }

    public Environment(WrappersProvider nativeConverter, GlobalScope global) {
        if (nativeConverter == null) nativeConverter = new NativeWrapperProvider(this);
        if (global == null) global = new GlobalScope();

        this.wrappers = nativeConverter;
        this.global = global;
    }
    public Environment() {
        this(null, null);
    }
}
