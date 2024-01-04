package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.Expose;
import me.topchetoeu.jscript.interop.ExposeConstructor;
import me.topchetoeu.jscript.interop.ExposeField;
import me.topchetoeu.jscript.interop.ExposeTarget;
import me.topchetoeu.jscript.interop.WrapperName;

@WrapperName("Number")
public class NumberLib {
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __EPSILON = Math.ulp(1.0);
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __MAX_SAFE_INTEGER = 9007199254740991.;
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __MIN_SAFE_INTEGER = -__MAX_SAFE_INTEGER;
    // lmao big number go brrr
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __MAX_VALUE = 179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368.;
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __MIN_VALUE = -__MAX_VALUE;
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __NaN = 0. / 0;
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __NEGATIVE_INFINITY = -1. / 0;
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __POSITIVE_INFINITY = 1. / 0;

    public final double value;

    @Expose(target = ExposeTarget.STATIC)
    public static boolean __isFinite(Arguments args) { return Double.isFinite(args.getDouble(0)); }
    @Expose(target = ExposeTarget.STATIC)
    public static boolean __isInfinite(Arguments args) { return Double.isInfinite(args.getDouble(0)); }
    @Expose(target = ExposeTarget.STATIC)
    public static boolean __isNaN(Arguments args) { return Double.isNaN(args.getDouble(0)); }
    @Expose(target = ExposeTarget.STATIC)
    public static boolean __isSafeInteger(Arguments args) {
        return args.getDouble(0) > __MIN_SAFE_INTEGER && args.getDouble(0) < __MAX_SAFE_INTEGER;
    }

    @Expose(target = ExposeTarget.STATIC)
    public static double __parseFloat(Arguments args) {
        return args.getDouble(0);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __parseInt(Arguments args) {
        return args.getLong(0);
    }

    @ExposeConstructor public static Object __constructor(Arguments args) {
        if (args.self instanceof ObjectValue) return new NumberLib(args.getDouble(0));
        else return args.getDouble(0);
    }
    @Expose public static String __toString(Arguments args) {
        return Values.toString(args.ctx, args.getDouble(0));
    }
    @Expose public static double __valueOf(Arguments args) {
        if (args.self instanceof NumberLib) return args.self(NumberLib.class).value;
        else return Values.toNumber(args.ctx, args.self);
    }

    public NumberLib(double val) {
        this.value = val;
    }
}
