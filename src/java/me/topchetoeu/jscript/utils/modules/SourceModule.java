package me.topchetoeu.jscript.utils.modules;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.runtime.Compiler;
import me.topchetoeu.jscript.runtime.environment.Environment;

public class SourceModule extends Module {
    public final Filename filename;
    public final String source;
    public final Environment ext;

    @Override
    protected Object onLoad(Environment env) {
        return Compiler.compile(env, filename, source).call(env);
    }

    public SourceModule(Filename filename, String source, Environment ext) {
        this.filename = filename;
        this.source = source;
        this.ext = ext;
    }
}
