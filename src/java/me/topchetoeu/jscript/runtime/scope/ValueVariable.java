package me.topchetoeu.jscript.runtime.scope;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.values.Value;

public class ValueVariable implements Variable {
    public boolean readonly;
    public Value value;

    @Override public boolean readonly() { return readonly; }
    @Override public final Value get(Environment env) { return get(); }
    @Override public final boolean set(Environment env, Value val) { return set(val); }

    public Value get() { return value; }
    public boolean set(Value val) {
        if (readonly) return false;
        this.value = val;
        return true;
    }

    public ValueVariable(boolean readonly, Value val) {
        this.readonly = readonly;
        this.value = val;
    }
}
