package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.interop.InitType;
import me.topchetoeu.jscript.interop.NativeConstructor;
import me.topchetoeu.jscript.interop.NativeInit;

public class TypeErrorLib extends ErrorLib {
    @NativeConstructor(thisArg = true) public static ObjectValue constructor(Context ctx, Object thisArg, Object message) throws InterruptedException {
        var target = ErrorLib.constructor(ctx, thisArg, message);
        target.defineProperty(ctx, "name", "TypeError");
        return target;
    }
    @NativeInit(InitType.PROTOTYPE) public static void init(Environment env, ObjectValue target) {
        target.defineProperty(null, env.symbol("Symbol.typeName"), "TypeError");
        target.defineProperty(null, "name", "TypeError");
    }
}