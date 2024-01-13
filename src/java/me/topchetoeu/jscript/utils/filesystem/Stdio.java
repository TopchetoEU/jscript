package me.topchetoeu.jscript.utils.filesystem;

import me.topchetoeu.jscript.core.engine.Extensions;
import me.topchetoeu.jscript.core.engine.values.Symbol;
import me.topchetoeu.jscript.core.exceptions.EngineException;

public class Stdio {
    public static final Symbol STDIN = Symbol.get("IO.stdin");
    public static final Symbol STDOUT = Symbol.get("IO.stdout");
    public static final Symbol STDERR = Symbol.get("IO.stderr");

    public static File stdout(Extensions exts) {
        if (exts.hasNotNull(STDOUT)) return exts.get(STDOUT);
        else throw EngineException.ofError("stdout is not supported.");
    }
    public static File stdin(Extensions exts) {
        if (exts.hasNotNull(STDIN)) return exts.get(STDIN);
        else throw EngineException.ofError("stdin is not supported.");
    }
    public static File stderr(Extensions exts) {
        if (exts.hasNotNull(STDERR)) return exts.get(STDERR);
        else throw EngineException.ofError("stderr is not supported.");
    }
}
