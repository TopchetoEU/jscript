package me.topchetoeu.jscript.engine.modules;

import java.io.File;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.CallContext.DataKey;
import me.topchetoeu.jscript.engine.scope.Variable;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.interop.NativeGetter;
import me.topchetoeu.jscript.interop.NativeSetter;

public class Module {
    public class ExportsVariable implements Variable {
        @Override
        public boolean readonly() { return false; }
        @Override
        public Object get(CallContext ctx) { return exports; }
        @Override
        public void set(CallContext ctx, Object val) { exports = val; }
    }

    public static DataKey<Module> KEY = new DataKey<>();

    public final String filename;
    public final String source;
    public final String name;
    private Object exports = new ObjectValue();
    private boolean executing = false;

    @NativeGetter("name")
    public String name() { return name; }
    @NativeGetter("exports")
    public Object exports() { return exports; }
    @NativeSetter("exports")
    public void setExports(Object val) { exports = val; }

    public void execute(CallContext ctx) throws InterruptedException {
        if (executing) return;

        executing = true;
        var scope = ctx.engine().global().globalChild();
        scope.define(null, "module", true, this);
        scope.define("exports", new ExportsVariable());

        var parent = new File(filename).getParentFile();
        if (parent == null) parent = new File(".");

        ctx.engine().compile(scope, filename, source).call(ctx.copy().setData(KEY, this), null);
        executing = false;
    }

    public Module(String name, String filename, String source) {
        this.name = name;
        this.filename = filename;
        this.source = source;
    }
}
