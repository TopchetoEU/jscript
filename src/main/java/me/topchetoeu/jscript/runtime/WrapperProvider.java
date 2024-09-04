package me.topchetoeu.jscript.runtime;

import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.ObjectValue;

public interface WrapperProvider {
    public ObjectValue getProto(Class<?> obj);
    public ObjectValue getNamespace(Class<?> obj);
    public FunctionValue getConstr(Class<?> obj);

    public WrapperProvider fork(Environment env);
}
