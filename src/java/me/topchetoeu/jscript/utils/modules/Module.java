package me.topchetoeu.jscript.utils.modules;

import me.topchetoeu.jscript.runtime.environment.Environment;

public abstract class Module {
    private Object value;
    private boolean loaded;

    public Object value() { return value; }
    public boolean loaded() { return loaded; }

    protected abstract Object onLoad(Environment ctx);

    public void load(Environment ctx) {
        if (loaded) return;
        this.value = onLoad(ctx);
        this.loaded = true;
    }
}

