package me.topchetoeu.jscript.core.scope;

import me.topchetoeu.jscript.core.Context;
import me.topchetoeu.jscript.core.values.Values;

public class ValueVariable implements Variable {
    public boolean readonly;
    public Object value;

    @Override
    public boolean readonly() { return readonly; }

    @Override
    public Object get(Context ctx) {
        return value;
    }

    @Override
    public void set(Context ctx, Object val) {
        if (readonly) return;
        this.value = Values.normalize(ctx, val);
    }
    
    public ValueVariable(boolean readonly, Object val) {
        this.readonly = readonly;
        this.value = val;
    }
}
