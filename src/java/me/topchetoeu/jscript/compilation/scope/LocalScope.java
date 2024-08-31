package me.topchetoeu.jscript.compilation.scope;

import me.topchetoeu.jscript.common.parsing.Location;

public class LocalScope extends Scope {
    private final VariableList locals = new VariableList();

    @Override public int offset() {
        if (parent != null) return parent.offset() + locals.size();
        else return locals.size();
    }

    @Override public VariableDescriptor define(String name, boolean readonly, Location loc) {
        if (locals.has(name)) throw alreadyDefinedErr(loc, name);

        return parent.define(name, readonly, loc);
    }
    @Override public VariableDescriptor defineStrict(String name, boolean readonly, Location loc) {
        if (locals.has(name)) throw alreadyDefinedErr(loc, name);
        return locals.add(name, readonly);
    }

    @Override public VariableDescriptor get(String name, boolean capture) {
        var res = locals.get(name);

        if (res != null) return res;
        if (parent != null) return parent.get(name, capture);

        return null;
    }

    @Override public boolean end() {
        if (!super.end()) return false;

        this.locals.freeze();
        return true;
    }

    public Iterable<VariableDescriptor> all() {
        return () -> locals.iterator();
    }

    public LocalScope(Scope parent) {
        super(parent);
    }
}
