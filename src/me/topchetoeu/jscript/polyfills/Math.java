package me.topchetoeu.jscript.polyfills;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.interop.Native;

public class Math {
    @Native
    public static final double E = java.lang.Math.E;
    @Native
    public static final double PI = java.lang.Math.PI;
    @Native
    public static final double SQRT2 = java.lang.Math.sqrt(2);
    @Native
    public static final double SQRT1_2 = java.lang.Math.sqrt(.5);
    @Native
    public static final double LN2 = java.lang.Math.log(2);
    @Native
    public static final double LN10 = java.lang.Math.log(10);
    @Native
    public static final double LOG2E = java.lang.Math.log(java.lang.Math.E) / LN2;
    @Native
    public static final double LOG10E = java.lang.Math.log10(java.lang.Math.E);

    @Native
    public static double asin(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.asin(x);
    }
    @Native
    public static double acos(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.acos(x);
    }
    @Native
    public static double atan(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.atan(x);
    }
    @Native
    public static double atan2(CallContext ctx, double y, double x) throws InterruptedException {
        double _y = y;
        double _x = x;
        if (_x == 0) {
            if (_y == 0) return Double.NaN;
            return java.lang.Math.signum(_y) * java.lang.Math.PI / 2;
        }
        else {
            var val = java.lang.Math.atan(_y / _x);
            if (_x > 0) return val;
            else if (_y < 0) return val - java.lang.Math.PI;
            else return val + java.lang.Math.PI;
        }

    }

    @Native
    public static double asinh(CallContext ctx, double x) throws InterruptedException {
        double _x = x;
        return java.lang.Math.log(_x + java.lang.Math.sqrt(_x * _x + 1));
    }
    @Native
    public static double acosh(CallContext ctx, double x) throws InterruptedException {
        double _x = x;
        return java.lang.Math.log(_x + java.lang.Math.sqrt(_x * _x - 1));
    }
    @Native
    public static double atanh(CallContext ctx, double x) throws InterruptedException {
        double _x = x;
        if (_x <= -1 || _x >= 1) return Double.NaN;
        return .5 * java.lang.Math.log((1 + _x) / (1 - _x));
    }

    @Native
    public static double sin(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.sin(x);
    }
    @Native
    public static double cos(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.cos(x);
    }
    @Native
    public static double tan(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.tan(x);
    }

    @Native
    public static double sinh(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.sinh(x);
    }
    @Native
    public static double cosh(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.cosh(x);
    }
    @Native
    public static double tanh(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.tanh(x);
    }

    @Native
    public static double sqrt(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.sqrt(x);
    }
    @Native
    public static double cbrt(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.cbrt(x);
    }

    @Native
    public static double hypot(CallContext ctx, double... vals) throws InterruptedException {
        var res = 0.;
        for (var el : vals) {
            var val = el;
            res += val * val;
        }
        return java.lang.Math.sqrt(res);
    }
    @Native
    public static int imul(CallContext ctx, double a, double b) throws InterruptedException {
        return (int)a * (int)b;
    }

    @Native
    public static double exp(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.exp(x);
    }
    @Native
    public static double expm1(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.expm1(x);
    }
    @Native
    public static double pow(CallContext ctx, double x, double y) throws InterruptedException {
        return java.lang.Math.pow(x, y);
    }

    @Native
    public static double log(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.log(x);
    }
    @Native
    public static double log10(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.log10(x);
    }
    @Native
    public static double log1p(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.log1p(x);
    }
    @Native
    public static double log2(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.log(x) / LN2;
    }

    @Native
    public static double ceil(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.ceil(x);
    }
    @Native
    public static double floor(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.floor(x);
    }
    @Native
    public static double round(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.round(x);
    }
    @Native
    public static float fround(CallContext ctx, double x) throws InterruptedException {
        return (float)x;
    }
    @Native
    public static double trunc(CallContext ctx, double x) throws InterruptedException {
        var _x = x;
        return java.lang.Math.floor(java.lang.Math.abs(_x)) * java.lang.Math.signum(_x);
    }
    @Native
    public static double abs(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.abs(x);
    }

    @Native
    public static double max(CallContext ctx, double... vals) throws InterruptedException {
        var res = Double.NEGATIVE_INFINITY;

        for (var el : vals) {
            var val = el;
            if (val > res) res = val;
        }

        return res;
    }
    @Native
    public static double min(CallContext ctx, double... vals) throws InterruptedException {
        var res = Double.POSITIVE_INFINITY;

        for (var el : vals) {
            var val = el;
            if (val < res) res = val;
        }

        return res;
    }

    @Native
    public static double sign(CallContext ctx, double x) throws InterruptedException {
        return java.lang.Math.signum(x);
    }

    @Native
    public static double random() {
        return java.lang.Math.random();
    }
    @Native
    public static int clz32(CallContext ctx, double x) throws InterruptedException {
        return Integer.numberOfLeadingZeros((int)x);
    }
}
