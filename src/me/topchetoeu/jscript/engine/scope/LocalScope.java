package me.topchetoeu.jscript.engine.scope;

import java.util.ArrayList;

public class LocalScope {
    private String[] names;
    public final ValueVariable[] captures;
    public final ValueVariable[] locals;
    public final ArrayList<ValueVariable> catchVars = new ArrayList<>();

    public ValueVariable get(int i) {
        if (i >= locals.length) return catchVars.get(i - locals.length);
        if (i >= 0) return locals[i];
        else return captures[~i];
    }

    public String[] getNames() {
        var res = new String[locals.length];

        for (var i = 0; i < locals.length; i++) {
            if (names == null || i >= names.length) res[i] = "local_" + i;
            else res[i] = names[i];
        }

        return res;
    }
    public void setNames(String[] names) {
        this.names = names;
    }

    public int size() {
        return captures.length + locals.length;
    }

    public void toGlobal(GlobalScope global) {
        var names = getNames();
        for (var i = 0; i < names.length; i++) {
            global.define(names[i], locals[i]);
        }
    }

    public LocalScope(int n, ValueVariable[] captures) {
        locals = new ValueVariable[n];
        this.captures = captures;

        for (int i = 0; i < n; i++) {
            locals[i] = new ValueVariable(false, null);
        }
    }
}
