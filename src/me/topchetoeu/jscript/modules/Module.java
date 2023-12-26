package me.topchetoeu.jscript.modules;

import me.topchetoeu.jscript.engine.Context;

public abstract class Module {
    private Object value;
    private boolean loaded;

    public Object value() { return value; }
    public boolean loaded() { return loaded; }

    protected abstract Object onLoad(Context ctx);

    public void load(Context ctx) {
        if (loaded) return;
        this.value = onLoad(ctx);
        this.loaded = true;
    }
}

