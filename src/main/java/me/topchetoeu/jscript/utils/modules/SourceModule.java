package me.topchetoeu.jscript.utils.modules;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.runtime.Context;
import me.topchetoeu.jscript.runtime.Extensions;

public class SourceModule extends Module {
    public final Filename filename;
    public final String source;
    public final Extensions ext;

    @Override
    protected Object onLoad(Context ctx) {
        var res = new Context(ext).compile(filename, source);
        return res.call(ctx);
    }

    public SourceModule(Filename filename, String source, Extensions ext) {
        this.filename = filename;
        this.source = source;
        this.ext = ext;
    }
}
