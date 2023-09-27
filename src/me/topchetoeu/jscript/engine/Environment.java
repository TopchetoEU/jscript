package me.topchetoeu.jscript.engine;

import java.util.HashMap;

import me.topchetoeu.jscript.engine.scope.GlobalScope;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Symbol;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeGetter;
import me.topchetoeu.jscript.interop.NativeSetter;
import me.topchetoeu.jscript.interop.NativeWrapperProvider;

public class Environment {
    private HashMap<String, ObjectValue> prototypes = new HashMap<>();
    public GlobalScope global;
    public WrappersProvider wrappersProvider;
    /** NOTE: This is not the register for Symbol.for, but for the symbols like Symbol.iterator */
    public HashMap<String, Symbol> symbols = new HashMap<>();

    @Native public FunctionValue compile;
    @Native public FunctionValue regexConstructor = new NativeFunction("RegExp", (ctx, thisArg, args) -> {
        throw EngineException.ofError("Regular expressions not supported.").setContext(ctx);
    });
    @Native public ObjectValue proto(String name) {
        return prototypes.get(name);
    }
    @Native public void setProto(String name, ObjectValue val) {
        prototypes.put(name, val);
    }

    @Native public Symbol symbol(String name) {
        if (symbols.containsKey(name))
            return symbols.get(name);
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
        var res = new Environment(compile, wrappersProvider, global);
        res.regexConstructor = regexConstructor;
        res.prototypes = new HashMap<>(prototypes);
        return res;
    }
    @Native public Environment child() {
        var res = fork();
        res.global = res.global.globalChild();
        return res;
    }

    public Environment(FunctionValue compile, WrappersProvider nativeConverter, GlobalScope global) {
        if (compile == null) compile = new NativeFunction("compile", (ctx, thisArg, args) -> args.length == 0 ? "" : args[0]);
        if (nativeConverter == null) nativeConverter = new NativeWrapperProvider(this);
        if (global == null) global = new GlobalScope();

        this.wrappersProvider = nativeConverter;
        this.compile = compile;
        this.global = global;
    }
}
