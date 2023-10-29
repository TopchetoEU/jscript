package me.topchetoeu.jscript.engine;

import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;

public interface WrappersProvider {
    public ObjectValue getProto(Class<?> obj);
    public ObjectValue getNamespace(Class<?> obj);
    public FunctionValue getConstr(Class<?> obj);

    public WrappersProvider fork(Environment env);
}
