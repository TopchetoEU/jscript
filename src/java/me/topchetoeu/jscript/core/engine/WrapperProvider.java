package me.topchetoeu.jscript.core.engine;

import me.topchetoeu.jscript.core.engine.values.FunctionValue;
import me.topchetoeu.jscript.core.engine.values.ObjectValue;

public interface WrapperProvider {
    public ObjectValue getProto(Class<?> obj);
    public ObjectValue getNamespace(Class<?> obj);
    public FunctionValue getConstr(Class<?> obj);

    public WrapperProvider fork(Environment env);
}
