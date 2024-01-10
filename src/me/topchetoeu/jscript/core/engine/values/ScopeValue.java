package me.topchetoeu.jscript.core.engine.values;

import java.util.HashMap;
import java.util.List;

import me.topchetoeu.jscript.core.engine.Context;
import me.topchetoeu.jscript.core.engine.scope.ValueVariable;

public class ScopeValue extends ObjectValue {
    public final ValueVariable[] variables;
    public final HashMap<String, Integer> names = new HashMap<>();

    @Override
    protected Object getField(Context ctx, Object key) {
        if (names.containsKey(key)) return variables[names.get(key)].get(ctx);
        return super.getField(ctx, key);
    }
    @Override
    protected boolean setField(Context ctx, Object key, Object val) {
        if (names.containsKey(key)) {
            variables[names.get(key)].set(ctx, val);
            return true;
        }

        var proto = getPrototype(ctx);
        if (proto != null && proto.hasMember(ctx, key, false) && proto.setField(ctx, key, val)) return true;

        return super.setField(ctx, key, val);
    }
    @Override
    protected void deleteField(Context ctx, Object key) {
        if (names.containsKey(key)) return;
        super.deleteField(ctx, key);
    }
    @Override
    protected boolean hasField(Context ctx, Object key) {
        if (names.containsKey(key)) return true;
        return super.hasField(ctx, key);
    }
    @Override
    public List<Object> keys(boolean includeNonEnumerable) {
        var res = super.keys(includeNonEnumerable);
        res.addAll(names.keySet());
        return res;
    }

    public ScopeValue(ValueVariable[] variables, String[] names) {
        this.variables = variables;
        for (var i = 0; i < names.length && i < variables.length; i++) {
            this.names.put(names[i], i);
            this.nonConfigurableSet.add(names[i]);
        }
    }
}
