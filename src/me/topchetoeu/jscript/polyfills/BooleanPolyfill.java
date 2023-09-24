package me.topchetoeu.jscript.polyfills;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeConstructor;

public class BooleanPolyfill {
    @NativeConstructor(thisArg = true) public static Object constructor(Context ctx, Object thisArg, Object val) {
        val = Values.toBoolean(val);
        if (thisArg instanceof ObjectValue) {
            ((ObjectValue)thisArg).defineProperty(ctx, "value", val);
            return null;
        }
        else return val;
    }
    @Native(thisArg = true) public static String toString(Context ctx, Object thisArg) {
        return Values.toBoolean(thisArg) ? "true" : "false";
    }
    @Native(thisArg = true) public static boolean valueOf(Context ctx, Object thisArg) {
        return Values.toBoolean(thisArg);
    }
}
