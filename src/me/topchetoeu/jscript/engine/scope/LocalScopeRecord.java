package me.topchetoeu.jscript.engine.scope;

import java.util.ArrayList;

import me.topchetoeu.jscript.engine.Context;

public class LocalScopeRecord implements ScopeRecord {
    public final LocalScopeRecord parent;
    public final GlobalScope global;

    private final ArrayList<String> captures = new ArrayList<>();
    private final ArrayList<String> locals = new ArrayList<>();

    public String[] captures() {
        return captures.toArray(String[]::new);
    }
    public String[] locals() {
        return locals.toArray(String[]::new);
    }

    @Override
    public LocalScopeRecord parent() { return parent; }

    public LocalScopeRecord child() {
        return new LocalScopeRecord(this, global);
    }

    public int localsCount() {
        return locals.size();
    }
    public int capturesCount() {
        return captures.size();
    }

    public int[] getCaptures() {
        var buff = new int[captures.size()];
        var i = 0;

        for (var name : captures) {
            var index = parent.getKey(name);
            if (index instanceof Integer) buff[i++] = (int)index;
        }

        var res = new int[i];
        System.arraycopy(buff, 0, res, 0, i);

        return res;
    }

    public Object getKey(String name) {
        var capI = captures.indexOf(name);
        var locI = locals.lastIndexOf(name);
        if (locI >= 0) return locI;
        if (capI >= 0) return ~capI;
        if (parent != null) {
            var res = parent.getKey(name);
            if (res != null && res instanceof Integer) {
                captures.add(name);
                return -captures.size();
            }
        }

        return name;
    }
    public boolean has(Context ctx, String name) {
        return
            global.has(ctx, name) ||
            locals.contains(name) ||
            parent != null && parent.has(ctx, name);
    }
    public Object define(String name, boolean force) {
        if (!force && locals.contains(name)) return locals.indexOf(name);
        locals.add(name);
        return locals.size() - 1;
    }
    public Object define(String name) {
        return define(name, false);
    }
    public void undefine() {
        locals.remove(locals.size() - 1);
    }

    public LocalScopeRecord(GlobalScope global) {
        this.parent = null;
        this.global = global;
    }
    public LocalScopeRecord(LocalScopeRecord parent, GlobalScope global) {
        this.parent = parent;
        this.global = global;
    }
}
