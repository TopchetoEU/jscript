package me.topchetoeu.jscript.engine.scope;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.values.Values;

public class ValueVariable implements Variable {
    public boolean readonly;
    public Object value;

    @Override
    public boolean readonly() { return readonly; }

    @Override
    public Object get(CallContext ctx) {
        return value;
    }

    @Override
    public void set(CallContext ctx, Object val) {
        if (readonly) return;
        this.value = Values.normalize(val);
    }
    
    public ValueVariable(boolean readonly, Object val) {
        this.readonly = readonly;
        this.value = val;
    }
}
