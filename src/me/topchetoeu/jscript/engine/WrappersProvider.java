package me.topchetoeu.jscript.engine;

import me.topchetoeu.jscript.engine.values.ObjectValue;

public interface WrappersProvider {
    public ObjectValue getProto(Class<?> obj);
    public ObjectValue getConstr(Class<?> obj);
}
