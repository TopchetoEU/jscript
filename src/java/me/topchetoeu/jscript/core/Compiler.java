package me.topchetoeu.jscript.core;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.core.exceptions.EngineException;

public interface Compiler {
    public Key<Compiler> KEY = new Key<>();

    public FunctionBody compile(Filename filename, String source);

    public static Compiler get(Extensions ext) {
        return ext.get(KEY, (filename, src) -> {
            throw EngineException.ofError("No compiler attached to engine.");
        });
    }
}
