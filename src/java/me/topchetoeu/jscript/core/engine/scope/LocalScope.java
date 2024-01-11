package me.topchetoeu.jscript.core.engine.scope;

import java.util.ArrayList;

public class LocalScope {
    public final ValueVariable[] captures;
    public final ValueVariable[] locals;
    public final ArrayList<ValueVariable> catchVars = new ArrayList<>();

    public ValueVariable get(int i) {
        if (i >= locals.length) return catchVars.get(i - locals.length);
        if (i >= 0) return locals[i];
        else return captures[~i];
    }


    public int size() {
        return captures.length + locals.length;
    }

    public LocalScope(int n, ValueVariable[] captures) {
        locals = new ValueVariable[n];
        this.captures = captures;

        for (int i = 0; i < n; i++) {
            locals[i] = new ValueVariable(false, null);
        }
    }
}
