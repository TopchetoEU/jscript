package me.topchetoeu.jscript.core;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.core.exceptions.EngineException;
import me.topchetoeu.jscript.core.scope.ValueVariable;
import me.topchetoeu.jscript.core.values.CodeFunction;

public interface Compiler {
    public Key<Compiler> KEY = new Key<>();

    public FunctionBody compile(Filename filename, String source);

    public static Compiler get(Extensions ext) {
        return ext.get(KEY, (filename, src) -> {
            throw EngineException.ofError("No compiler attached to engine.");
        });
    }

    public static CodeFunction compile(Environment env, Filename filename, String raw) {
        return new CodeFunction(env, filename.toString(), Compiler.get(env).compile(filename, raw), new ValueVariable[0]);
    }
}
