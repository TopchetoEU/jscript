package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Environment;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.interop.InitType;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeConstructor;
import me.topchetoeu.jscript.interop.NativeInit;

@Native("SyntaxError") public class SyntaxErrorLib extends ErrorLib {
    @NativeConstructor(thisArg = true) public static ObjectValue constructor(Context ctx, Object thisArg, Object message) {
        var target = ErrorLib.constructor(ctx, thisArg, message);
        return target;
    }
    @NativeInit(InitType.PROTOTYPE) public static void init(Environment env, ObjectValue target) {
        target.defineProperty(null, "name", "SyntaxError");
    }
}