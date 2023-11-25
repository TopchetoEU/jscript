package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeConstructor;

@Native("Boolean") public class BooleanLib {
    public static final BooleanLib TRUE = new BooleanLib(true);
    public static final BooleanLib FALSE = new BooleanLib(false);

    public final boolean value;

    @NativeConstructor(thisArg = true) public static Object constructor(Context ctx, Object thisArg, Object val) {
        val = Values.toBoolean(val);
        if (thisArg instanceof ObjectValue) return (boolean)val ? TRUE : FALSE;
        else return val;
    }
    @Native(thisArg = true) public static String toString(Context ctx, Object thisArg) {
        return Values.toBoolean(thisArg) ? "true" : "false";
    }
    @Native(thisArg = true) public static boolean valueOf(Context ctx, Object thisArg) {
        return Values.toBoolean(thisArg);
    }

    public BooleanLib(boolean val) {
        this.value = val;
    }
}
