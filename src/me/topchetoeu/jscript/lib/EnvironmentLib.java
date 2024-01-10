package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.core.engine.Environment;
import me.topchetoeu.jscript.core.engine.values.FunctionValue;
import me.topchetoeu.jscript.core.engine.values.ObjectValue;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeType;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("Environment")
public class EnvironmentLib {
    private Environment env;

    @Expose(value = "@@env", type = ExposeType.GETTER)
    public Environment __env() { return env; }

    @Expose(type = ExposeType.GETTER)
    public int __id(Arguments args) {
        return env.hashCode();
    }
    @Expose(type = ExposeType.GETTER)
    public ObjectValue __global(Arguments args) {
        return env.global.obj;
    }

    @Expose(type = ExposeType.GETTER)
    public FunctionValue __compile() {
        return Environment.compileFunc(env);
    }
    @Expose(type = ExposeType.SETTER)
    public void __compile(Arguments args) {
        env.add(Environment.COMPILE_FUNC, args.convert(0, FunctionValue.class));
    }

    public EnvironmentLib(Environment env) {
        this.env = env;
    }
}
