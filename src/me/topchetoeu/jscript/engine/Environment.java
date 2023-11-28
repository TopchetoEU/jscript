package me.topchetoeu.jscript.engine;

import java.util.HashMap;
import java.util.TreeSet;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.engine.scope.GlobalScope;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.filesystem.RootFilesystem;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeGetter;
import me.topchetoeu.jscript.interop.NativeSetter;
import me.topchetoeu.jscript.interop.NativeWrapperProvider;
import me.topchetoeu.jscript.parsing.Parsing;
import me.topchetoeu.jscript.permissions.Permission;
import me.topchetoeu.jscript.permissions.PermissionsProvider;

public class Environment implements PermissionsProvider {
    private HashMap<String, ObjectValue> prototypes = new HashMap<>();

    public final Data data = new Data();
    public static final HashMap<String, Symbol> symbols = new HashMap<>();

    public GlobalScope global;
    public WrappersProvider wrappers;
    public PermissionsProvider permissions = null;
    public final RootFilesystem filesystem = new RootFilesystem(this);

    private static int nextId = 0;

    @Native public int id = ++nextId;

    @Native public FunctionValue compile = new NativeFunction("compile", (ctx, thisArg, args) -> {
        var source = Values.toString(ctx, args[0]);
        var filename = Values.toString(ctx, args[1]);
        var env = Values.wrapper(args[2], Environment.class);
        var res = new ObjectValue();

        res.defineProperty(ctx, "function", Parsing.compile(Engine.functions, new TreeSet<>(), env, Filename.parse(filename), source));
        res.defineProperty(ctx, "mapChain", new ArrayValue());

        return res;
    });
    @Native public FunctionValue regexConstructor = new NativeFunction("RegExp", (ctx, thisArg, args) -> {
        throw EngineException.ofError("Regular expressions not supported.").setCtx(ctx.environment(), ctx.engine);
    });

    public Environment addData(Data data) {
        this.data.addAll(data);
        return this;
    }

    @Native public ObjectValue proto(String name) {
        return prototypes.get(name);
    }
    @Native public void setProto(String name, ObjectValue val) {
        prototypes.put(name, val);
    }

    @Native public Symbol symbol(String name) {
        if (symbols.containsKey(name)) return symbols.get(name);
        else {
            var res = new Symbol(name);
            symbols.put(name, res);
            return res;
        }
    }

    @NativeGetter("global") public ObjectValue getGlobal() {
        return global.obj;
    }
    @NativeSetter("global") public void setGlobal(ObjectValue val) {
        global = new GlobalScope(val);
    }

    @Native public Environment fork() {
        var res = new Environment(compile, null, global);
        res.wrappers = wrappers.fork(res);
        res.regexConstructor = regexConstructor;
        res.prototypes = new HashMap<>(prototypes);
        return res;
    }
    @Native public Environment child() {
        var res = fork();
        res.global = res.global.globalChild();
        return res;
    }

    @Override public boolean hasPermission(Permission perm, char delim) {
        return permissions == null || permissions.hasPermission(perm, delim);
    }
    @Override public boolean hasPermission(Permission perm) {
        return permissions == null || permissions.hasPermission(perm);
    }

    public Context context(Engine engine) {
        return new Context(engine).pushEnv(this);
    }

    public Environment(FunctionValue compile, WrappersProvider nativeConverter, GlobalScope global) {
        if (compile != null) this.compile = compile;
        if (nativeConverter == null) nativeConverter = new NativeWrapperProvider(this);
        if (global == null) global = new GlobalScope();

        this.wrappers = nativeConverter;
        this.global = global;
    }
}
