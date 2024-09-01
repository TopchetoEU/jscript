package me.topchetoeu.jscript.compilation.scope;

import me.topchetoeu.jscript.common.parsing.Location;

public final class LocalScope extends Scope {
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

    @Override public boolean has(String name) {
        if (locals.has(name)) return true;
        if (parent != null) return parent.has(name);

        return false;
    }

    @Override public boolean end() {
        if (!super.end()) return false;

        this.locals.freeze();
        return true;
    }

    @Override public int localsCount() {
        if (parent == null) return 0;
        else return parent.localsCount();
    }
    @Override public int capturesCount() {
        if (parent == null) return 0;
        else return parent.capturesCount();
    }
    @Override public int allocCount() {
        return locals.size();
    }

    public Iterable<VariableDescriptor> all() {
        return () -> locals.iterator();
    }


    public LocalScope(Scope parent) {
        super(parent);
    }
}
