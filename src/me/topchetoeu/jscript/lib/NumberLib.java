package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeConstructor;

@Native("Number") public class NumberLib {
    @Native public static final double EPSILON = java.lang.Math.ulp(1.0);
    @Native public static final double MAX_SAFE_INTEGER = 9007199254740991.;
    @Native public static final double MIN_SAFE_INTEGER = -MAX_SAFE_INTEGER;
    // lmao big number go brrr
    @Native public static final double MAX_VALUE = 179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368.;
    @Native public static final double MIN_VALUE = -MAX_VALUE;
    @Native public static final double NaN = 0. / 0;
    @Native public static final double NEGATIVE_INFINITY = -1. / 0;
    @Native public static final double POSITIVE_INFINITY = 1. / 0;

    public final double value;

    @Native public static boolean isFinite(Context ctx, double val) { return Double.isFinite(val); }
    @Native public static boolean isInfinite(Context ctx, double val) { return Double.isInfinite(val); }
    @Native public static boolean isNaN(Context ctx, double val) { return Double.isNaN(val); }
    @Native public static boolean isSafeInteger(Context ctx, double val) {
        return val > MIN_SAFE_INTEGER && val < MAX_SAFE_INTEGER;
    }

    @Native public static double parseFloat(Context ctx, String val) {
        return Values.toNumber(ctx, val);
    }
    @Native public static double parseInt(Context ctx, String val) {
        return (long)Values.toNumber(ctx, val);
    }

    @NativeConstructor(thisArg = true) public static Object constructor(Context ctx, Object thisArg, Object val) {
        val = Values.toNumber(ctx, val);
        if (thisArg instanceof ObjectValue) return new NumberLib((double)val);
        else return val;
    }
    @Native(thisArg = true) public static String toString(Context ctx, Object thisArg) {
        return Values.toString(ctx, Values.toNumber(ctx, thisArg));
    }
    @Native(thisArg = true) public static double valueOf(Context ctx, Object thisArg) {
        if (thisArg instanceof NumberLib) return ((NumberLib)thisArg).value;
        else return Values.toNumber(ctx, thisArg);
    }

    public NumberLib(double val) {
        this.value = val;
    }
}
