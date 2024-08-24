package me.topchetoeu.jscript.runtime;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.environment.Key;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.scope.ValueVariable;
import me.topchetoeu.jscript.runtime.values.CodeFunction;

public interface Compiler {
    public Key<Compiler> KEY = new Key<>();

    public FunctionBody compile(Filename filename, String source);

    public static Compiler get(Environment ext) {
        return ext.get(KEY, (filename, src) -> {
            throw EngineException.ofError("No compiler attached to engine.");
        });
    }

    public static CodeFunction compile(Environment env, Filename filename, String raw) {
        DebugContext.get(env).onSource(filename, raw);
        return new CodeFunction(env, filename.toString(), Compiler.get(env).compile(filename, raw), new ValueVariable[0]);
    }
}
