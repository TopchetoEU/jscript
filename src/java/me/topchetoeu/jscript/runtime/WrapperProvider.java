package me.topchetoeu.jscript.runtime;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;

public interface WrapperProvider {
    public ObjectValue getProto(Class<?> obj);
    public ObjectValue getNamespace(Class<?> obj);
    public FunctionValue getConstr(Class<?> obj);

    public WrapperProvider fork(Environment env);
}
