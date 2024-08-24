package me.topchetoeu.jscript.runtime.scope;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.values.Values;

public class ValueVariable implements Variable {
    public boolean readonly;
    public Object value;

    @Override
    public boolean readonly() { return readonly; }

    @Override
    public Object get(Environment ext) {
        return value;
    }

    @Override
    public void set(Environment ext, Object val) {
        if (readonly) return;
        this.value = Values.normalize(ext, val);
    }
    
    public ValueVariable(boolean readonly, Object val) {
        this.readonly = readonly;
        this.value = val;
    }
}
