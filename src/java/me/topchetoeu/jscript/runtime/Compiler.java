package me.topchetoeu.jscript.runtime;

import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.common.parsing.Filename;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.functions.CodeFunction;

public interface Compiler {
    public static final Compiler DEFAULT = (env, filename, raw) -> {
        try {
            var res = JavaScript.compile(env, filename, raw);
            var body = res.body();
            DebugContext.get(env).onSource(filename, raw);
            registerFunc(env, body, res);
            return body;
        }
        catch (SyntaxException e) {
            throw EngineException.ofSyntax(e.loc + ": " + e.msg);
        }
    };

    public Key<Compiler> KEY = Key.of();

    public FunctionBody compile(Environment env, Filename filename, String source);

    public static Compiler get(Environment ext) {
        return ext.get(KEY, (env, filename, src) -> {
            throw EngineException.ofError("No compiler attached to engine.");
        });
    }

    static void registerFunc(Environment env, FunctionBody body, CompileResult res) {
        var map = res.map();

        DebugContext.get(env).onFunctionLoad(body, map);

        for (var i = 0; i < body.children.length; i++) {
            registerFunc(env, body.children[i], res.children.get(i));
        }
    }

    public static CodeFunction compileFunc(Environment env, Filename filename, String raw) {
        return new CodeFunction(env, filename.toString(), get(env).compile(env, filename, raw), new Value[0][]);
    }
}
