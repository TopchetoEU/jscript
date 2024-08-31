package me.topchetoeu.jscript.compilation.scope;

import me.topchetoeu.jscript.common.parsing.Location;

public final class GlobalScope extends Scope {
    @Override public VariableDescriptor define(String name, boolean readonly, Location loc) {
        return null;
    }
    @Override public VariableDescriptor defineStrict(String name, boolean readonly, Location loc) {
        return null;
    }
    @Override public VariableDescriptor get(String name, boolean capture) {
        return null;
    }
    @Override public int offset() {
        return 0;
    }
}
