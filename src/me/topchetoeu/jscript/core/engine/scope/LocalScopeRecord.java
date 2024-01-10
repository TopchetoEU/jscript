package me.topchetoeu.jscript.core.engine.scope;

import java.util.ArrayList;

public class LocalScopeRecord implements ScopeRecord {
    public final LocalScopeRecord parent;

    private final ArrayList<String> captures = new ArrayList<>();
    private final ArrayList<String> locals = new ArrayList<>();

    public String[] captures() {
        return captures.toArray(String[]::new);
    }
    public String[] locals() {
        return locals.toArray(String[]::new);
    }

    public LocalScopeRecord child() {
        return new LocalScopeRecord(this);
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

    public LocalScopeRecord() {
        this.parent = null;
    }
    public LocalScopeRecord(LocalScopeRecord parent) {
        this.parent = parent;
    }
}
