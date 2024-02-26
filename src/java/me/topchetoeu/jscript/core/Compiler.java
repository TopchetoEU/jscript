package me.topchetoeu.jscript.core;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.compilation.FunctionBody;

public interface Compiler {
    public FunctionBody compile(Filename filename, String source);
}
