package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeGetter;
import me.topchetoeu.jscript.interop.NativeSetter;

@Native("Environment")
public class EnvironmentLib {
    private Environment env;

    @NativeGetter("@@env") public Environment env() { return env; }

    @NativeGetter public int id() {
        return env.hashCode();
    }
    @NativeGetter public ObjectValue global() {
        return env.global.obj;
    }

    @NativeGetter public FunctionValue compile() {
        return Environment.compileFunc(env);
    }
    @NativeSetter public void compile(FunctionValue func) {
        env.add(Environment.COMPILE_FUNC, func);
    }

    public EnvironmentLib(Environment env) {
        this.env = env;
    }
}
