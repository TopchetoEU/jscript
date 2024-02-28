package me.topchetoeu.jscript.utils;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.FunctionBody;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.core.Compiler;
import me.topchetoeu.jscript.core.Extensions;
import me.topchetoeu.jscript.core.debug.DebugContext;

public class JSCompiler implements Compiler {
    public final Extensions ext;

    private void registerFunc(FunctionBody body, CompileResult res) {
        var map = res.map();
        DebugContext.get(ext).onFunctionLoad(body, map);

        for (var i = 0; i < body.children.length; i++) {
            registerFunc(body.children[i], res.children.get(i));
        }
    }

    @Override public FunctionBody compile(Filename filename, String source) {
        var res = Parsing.compile(filename, source);
        var func = res.body();
        registerFunc(func, res);

        return func;
    }

    public JSCompiler(Extensions ext) {
        this.ext = ext;
    }
}
