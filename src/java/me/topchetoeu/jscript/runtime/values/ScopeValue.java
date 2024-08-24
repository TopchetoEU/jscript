package me.topchetoeu.jscript.runtime.values;

import java.util.HashMap;
import java.util.List;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.scope.ValueVariable;

public class ScopeValue extends ObjectValue {
    public final ValueVariable[] variables;
    public final HashMap<String, Integer> names = new HashMap<>();

    @Override
    protected Object getField(Environment ext, Object key) {
        if (names.containsKey(key)) return variables[names.get(key)].get(ext);
        return super.getField(ext, key);
    }
    @Override
    protected boolean setField(Environment ext, Object key, Object val) {
        if (names.containsKey(key)) {
            variables[names.get(key)].set(ext, val);
            return true;
        }

        var proto = getPrototype(ext);
        if (proto != null && proto.hasMember(ext, key, false) && proto.setField(ext, key, val)) return true;

        return super.setField(ext, key, val);
    }
    @Override
    protected void deleteField(Environment ext, Object key) {
        if (names.containsKey(key)) return;
        super.deleteField(ext, key);
    }
    @Override
    protected boolean hasField(Environment ext, Object key) {
        if (names.containsKey(key)) return true;
        return super.hasField(ext, key);
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
