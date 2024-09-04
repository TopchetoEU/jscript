package me.topchetoeu.jscript.runtime.scope;

import me.topchetoeu.jscript.runtime.Extensions;
import me.topchetoeu.jscript.runtime.values.Values;

public class ValueVariable implements Variable {
    public boolean readonly;
    public Object value;

    @Override
    public boolean readonly() { return readonly; }

    @Override
    public Object get(Extensions ext) {
        return value;
    }

    @Override
    public void set(Extensions ext, Object val) {
        if (readonly) return;
        this.value = Values.normalize(ext, val);
    }
    
    public ValueVariable(boolean readonly, Object val) {
        this.readonly = readonly;
        this.value = val;
    }
}
