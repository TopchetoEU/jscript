package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.interop.Native;

@Native("Math") public class MathLib {
    @Native public static final double E = Math.E;
    @Native public static final double PI = Math.PI;
    @Native public static final double SQRT2 = Math.sqrt(2);
    @Native public static final double SQRT1_2 = Math.sqrt(.5);
    @Native public static final double LN2 = Math.log(2);
    @Native public static final double LN10 = Math.log(10);
    @Native public static final double LOG2E = Math.log(Math.E) / LN2;
    @Native public static final double LOG10E = Math.log10(Math.E);

    @Native public static double asin(double x) { return Math.asin(x); }
    @Native public static double acos(double x) { return Math.acos(x); }
    @Native public static double atan(double x) { return Math.atan(x); }
    @Native public static double atan2(double y, double x) {
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

    @Native public static double asinh(double x) { return Math.log(x + Math.sqrt(x * x + 1)); }
    @Native public static double acosh(double x) { return Math.log(x + Math.sqrt(x * x - 1)); }
    @Native public static double atanh(double x) { 
        if (x <= -1 || x >= 1) return Double.NaN;
        return .5 * Math.log((1 + x) / (1 - x));
    }

    @Native public static double sin(double x) { return Math.sin(x); }
    @Native public static double cos(double x) { return Math.cos(x); }
    @Native public static double tan(double x) { return Math.tan(x); }

    @Native public static double sinh(double x) { return Math.sinh(x); }
    @Native public static double cosh(double x) { return Math.cosh(x); }
    @Native public static double tanh(double x) { return Math.tanh(x); }

    @Native public static double sqrt(double x) { return Math.sqrt(x); }
    @Native public static double cbrt(double x) { return Math.cbrt(x); }

    @Native public static double hypot(double ...vals) {
        var res = 0.;
        for (var el : vals) {
            var val = el;
            res += val * val;
        }
        return Math.sqrt(res);
    }
    @Native public static int imul(double a, double b) { return (int)a * (int)b; }

    @Native public static double exp(double x) { return Math.exp(x); }
    @Native public static double expm1(double x) { return Math.expm1(x); }
    @Native public static double pow(double x, double y) { return Math.pow(x, y); }

    @Native public static double log(double x) { return Math.log(x); }
    @Native public static double log10(double x) { return Math.log10(x); }
    @Native public static double log1p(double x) { return Math.log1p(x); }
    @Native public static double log2(double x) { return Math.log(x) / LN2; }

    @Native public static double ceil(double x) { return Math.ceil(x); }
    @Native public static double floor(double x) { return Math.floor(x); }
    @Native public static double round(double x) { return Math.round(x); }
    @Native public static float fround(double x) { return (float)x; }
    @Native public static double trunc(double x) { return Math.floor(Math.abs(x)) * Math.signum(x); }
    @Native public static double abs(double x) { return Math.abs(x); }

    @Native public static double max(double ...vals) {
        var res = Double.NEGATIVE_INFINITY;

        for (var el : vals) {
            if (el > res) res = el;
        }

        return res;
    }
    @Native public static double min(double ...vals) {
        var res = Double.POSITIVE_INFINITY;

        for (var el : vals) {
            if (el < res) res = el;
        }

        return res;
    }

    @Native public static double sign(double x) { return Math.signum(x); }

    @Native public static double random() { return Math.random(); }
    @Native public static int clz32(double x) { return Integer.numberOfLeadingZeros((int)x); }
}
