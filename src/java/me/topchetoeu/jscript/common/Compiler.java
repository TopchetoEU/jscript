package me.topchetoeu.jscript.common;

import me.topchetoeu.jscript.common.parsing.Filename;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.ES5;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.environment.Key;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.scope.ValueVariable;
import me.topchetoeu.jscript.runtime.values.functions.CodeFunction;

public interface Compiler {
    public static final Compiler DEFAULT = (env, filename, raw) -> {
        var res = ES5.compile(filename, raw);
        var body = res.body();
        DebugContext.get(env).onSource(filename, raw);
        registerFunc(env, body, res);

        return body;
    };

    public Key<Compiler> KEY = new Key<>();

    public FunctionBody compile(Environment env, Filename filename, String source);

    public static Compiler get(Environment ext) {
        return ext.get(KEY, (env, filename, src) -> {
            throw EngineException.ofError("No compiler attached to engine.");
        });
    }

    private static void registerFunc(Environment env, FunctionBody body, CompileResult res) {
        var map = res.map();

        DebugContext.get(env).onFunctionLoad(body, map);

        for (var i = 0; i < body.children.length; i++) {
            registerFunc(env, body.children[i], res.children.get(i));
        }
    }

    public static CodeFunction compileFunc(Environment env, Filename filename, String raw) {
        return new CodeFunction(env, filename.toString(), get(env).compile(env, filename, raw), new ValueVariable[0]);
    }
}
