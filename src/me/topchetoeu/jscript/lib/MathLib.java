package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeField;
import me.topchetoeu.jscript.utils.interop.ExposeTarget;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("Math")
public class MathLib {
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __E = Math.E;
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __PI = Math.PI;
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __SQRT2 = Math.sqrt(2);
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __SQRT1_2 = Math.sqrt(.5);
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __LN2 = Math.log(2);
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __LN10 = Math.log(10);
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __LOG2E = Math.log(Math.E) / __LN2;
    @ExposeField(target = ExposeTarget.STATIC)
    public static final double __LOG10E = Math.log10(Math.E);

    @Expose(target = ExposeTarget.STATIC)
    public static double __asin(Arguments args) {
        return Math.asin(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __acos(Arguments args) {
        return Math.acos(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __atan(Arguments args) {
        return Math.atan(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __atan2(Arguments args) {
        var x = args.getDouble(1);
        var y = args.getDouble(0);

        if (x == 0) {
            if (y == 0) return Double.NaN;
            return Math.signum(y) * Math.PI / 2;
        }
        else {
            var val = Math.atan(y / x);
            if (x > 0) return val;
            else if (y < 0) return val - Math.PI;
            else return val + Math.PI;
        }

    }

    @Expose(target = ExposeTarget.STATIC)
    public static double __asinh(Arguments args) {
        var x = args.getDouble(0);
        return Math.log(x + Math.sqrt(x * x + 1));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __acosh(Arguments args) {
        var x = args.getDouble(0);
        return Math.log(x + Math.sqrt(x * x - 1));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __atanh(Arguments args) {
        var x = args.getDouble(0);

        if (x <= -1 || x >= 1) return Double.NaN;
        return .5 * Math.log((1 + x) / (1 - x));
    }

    @Expose(target = ExposeTarget.STATIC)
    public static double __sin(Arguments args) {
        return Math.sin(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __cos(Arguments args) {
        return Math.cos(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __tan(Arguments args) {
        return Math.tan(args.getDouble(0));
    }

    @Expose(target = ExposeTarget.STATIC)
    public static double __sinh(Arguments args) {
        return Math.sinh(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __cosh(Arguments args) {
        return Math.cosh(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __tanh(Arguments args) {
        return Math.tanh(args.getDouble(0));
    }

    @Expose(target = ExposeTarget.STATIC)
    public static double __sqrt(Arguments args) {
        return Math.sqrt(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __cbrt(Arguments args) {
        return Math.cbrt(args.getDouble(0));
    }

    @Expose(target = ExposeTarget.STATIC)
    public static double __hypot(Arguments args) {
        var res = 0.;
        for (var i = 0; i < args.n(); i++) {
            var val = args.getDouble(i);
            res += val * val;
        }
        return Math.sqrt(res);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static int __imul(Arguments args) { return args.getInt(0) * args.getInt(1); }

    @Expose(target = ExposeTarget.STATIC)
    public static double __exp(Arguments args) {
        return Math.exp(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __expm1(Arguments args) {
        return Math.expm1(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __pow(Arguments args) { return Math.pow(args.getDouble(0), args.getDouble(1)); }

    @Expose(target = ExposeTarget.STATIC)
    public static double __log(Arguments args) {
        return Math.log(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __log10(Arguments args) {
        return Math.log10(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __log1p(Arguments args) {
        return Math.log1p(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __log2(Arguments args) {
        return Math.log(args.getDouble(0)) / __LN2;
    }

    @Expose(target = ExposeTarget.STATIC)
    public static double __ceil(Arguments args) {
        return Math.ceil(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __floor(Arguments args) {
        return Math.floor(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __round(Arguments args) {
        return Math.round(args.getDouble(0));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static float __fround(Arguments args) {
        return (float)args.getDouble(0);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __trunc(Arguments args) {
        var x = args.getDouble(0);
        return Math.floor(Math.abs(x)) * Math.signum(x);
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __abs(Arguments args) {
        return Math.abs(args.getDouble(0));
    }

    @Expose(target = ExposeTarget.STATIC)
    public static double __max(Arguments args) {
        var res = Double.NEGATIVE_INFINITY;

        for (var i = 0; i < args.n(); i++) {
            var el = args.getDouble(i);
            if (el > res) res = el;
        }

        return res;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static double __min(Arguments args) {
        var res = Double.POSITIVE_INFINITY;

        for (var i = 0; i < args.n(); i++) {
            var el = args.getDouble(i);
            if (el < res) res = el;
        }

        return res;
    }

    @Expose(target = ExposeTarget.STATIC)
    public static double __sign(Arguments args) {
        return Math.signum(args.getDouble(0));
    }

    @Expose(target = ExposeTarget.STATIC)
    public static double __random() { return Math.random(); }
    @Expose(target = ExposeTarget.STATIC)
    public static int __clz32(Arguments args) {
        return Integer.numberOfLeadingZeros(args.getInt(0));
    }
}
