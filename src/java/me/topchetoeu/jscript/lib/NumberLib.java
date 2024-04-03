package me.topchetoeu.jscript.lib;

import java.text.NumberFormat;

import me.topchetoeu.jscript.runtime.values.ObjectValue;
import me.topchetoeu.jscript.runtime.values.Values;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeConstructor;
import me.topchetoeu.jscript.utils.interop.ExposeField;
import me.topchetoeu.jscript.utils.interop.ExposeTarget;
import me.topchetoeu.jscript.utils.interop.WrapperName;

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

    @Override public String toString() { return value + ""; }

    public NumberLib(double val) {
        this.value = val;
    }

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
        var radix = args.getInt(1, 10);

        if (radix < 2 || radix > 36) return Double.NaN;
        else {
            long res = 0;
            
            for (var c : args.getString(0).toCharArray()) {
                var digit = 0;

                if (c >= '0' && c <= '9') digit = c - '0';
                else if (c >= 'a' && c <= 'z') digit = c - 'a' + 10;
                else if (c >= 'A' && c <= 'Z') digit = c - 'A' + 10;
                else break;

                if (digit > radix) break;

                res *= radix;
                res += digit;
            }

            return res;
        }
    }

    @ExposeConstructor public static Object __constructor(Arguments args) {
        if (args.self instanceof ObjectValue) return new NumberLib(args.getDouble(0));
        else return args.getDouble(0);
    }
    @Expose public static String __toString(Arguments args) {
        return Values.toString(args.ctx, args.self);
    }
    @Expose public static String __toFixed(Arguments args) {
        var digits = args.getInt(0, 0);

        var nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(digits);
        nf.setMaximumFractionDigits(digits);

        return nf.format(args.getDouble(-1));
    }
    @Expose public static double __valueOf(Arguments args) {
        if (Values.isWrapper(args.self, NumberLib.class)) return Values.wrapper(args.self, NumberLib.class).value;
        else return Values.toNumber(args.ctx, args.self);
    }
}
