package me.topchetoeu.jscript.utils.modules;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.core.engine.Context;
import me.topchetoeu.jscript.core.engine.Environment;

public class SourceModule extends Module {
    public final Filename filename;
    public final String source;
    public final Environment env;

    @Override
    protected Object onLoad(Context ctx) {
        var res = new Context(ctx.engine, env).compile(filename, source);
        return res.call(ctx);
    }

    public SourceModule(Filename filename, String source, Environment env) {
        this.filename = filename;
        this.source = source;
        this.env = env;
    }
}