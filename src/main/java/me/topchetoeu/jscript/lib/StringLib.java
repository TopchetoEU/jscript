package me.topchetoeu.jscript.lib;

import java.util.regex.Pattern;

import me.topchetoeu.jscript.runtime.Environment;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.ArrayValue;
import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.ObjectValue;
import me.topchetoeu.jscript.runtime.values.Symbol;
import me.topchetoeu.jscript.runtime.values.Values;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeConstructor;
import me.topchetoeu.jscript.utils.interop.ExposeTarget;
import me.topchetoeu.jscript.utils.interop.ExposeType;
import me.topchetoeu.jscript.utils.interop.WrapperName;

// TODO: implement index wrapping properly
@WrapperName("String")
public class StringLib {
    public final String value;

    @Override public String toString() { return value; }

    public StringLib(String val) {
        this.value = val;
    }

    private static String passThis(Arguments args, String funcName) {
        var val = args.self;
        if (Values.isWrapper(val, StringLib.class)) return Values.wrapper(val, StringLib.class).value;
        else if (val instanceof String) return (String)val;
        else throw EngineException.ofType(String.format("'%s' may only be called upon object and primitve strings.", funcName));
    }
    private static int normalizeI(int i, int len, boolean clamp) {
        if (i < 0) i += len;
        if (clamp) {
            if (i < 0) i = 0;
            if (i > len) i = len;
        }
        return i;
    }

    @Expose(type = ExposeType.GETTER)
    public static int __length(Arguments args) {
        return passThis(args, "length").length();
    }

    @Expose public static String __substring(Arguments args) {
        var val = passThis(args, "substring");
        var start = Math.max(0, Math.min(val.length(), args.getInt(0)));
        var end = Math.max(0, Math.min(val.length(), args.getInt(1, val.length())));

        if (end < start) {
            var tmp = end;
            end = start;
            start = tmp;
        }

        return val.substring(start, end);
    }
    @Expose public static String __substr(Arguments args) {
        var val = passThis(args, "substr");
        var start = normalizeI(args.getInt(0), val.length(), true);
        int end = normalizeI(args.getInt(1, val.length() - start) + start, val.length(), true);
        return val.substring(start, end);
    }

    @Expose public static String __toLowerCase(Arguments args) {
        return passThis(args, "toLowerCase").toLowerCase();
    }
    @Expose public static String __toUpperCase(Arguments args) {
        return passThis(args, "toUpperCase").toUpperCase();
    }

    @Expose public static String __charAt(Arguments args) {
        return passThis(args, "charAt").charAt(args.getInt(0)) + "";
    }
    @Expose public static double __charCodeAt(Arguments args) {
        var str = passThis(args, "charCodeAt");
        var i = args.getInt(0);
        if (i < 0 || i >= str.length()) return Double.NaN;
        else return str.charAt(i);
    }
    @Expose public static double __codePointAt(Arguments args) {
        var str = passThis(args, "codePointAt");
        var i = args.getInt(0);
        if (i < 0 || i >= str.length()) return Double.NaN;
        else return str.codePointAt(i);
    }

    @Expose public static boolean __startsWith(Arguments args) {
        return passThis(args, "startsWith").startsWith(args.getString(0), args.getInt(1));
    }
    @Expose public static boolean __endsWith(Arguments args) {
        return passThis(args, "endsWith").lastIndexOf(args.getString(0), args.getInt(1)) >= 0;
    }

    @Expose public static int __indexOf(Arguments args) {
        var val = passThis(args, "indexOf");
        var term = args.get(0);
        var start = args.getInt(1);
        var search = Values.getMember(args.ctx, term, Symbol.get("Symbol.search"));

        if (search instanceof FunctionValue) {
            return (int)Values.toNumber(args.ctx, Values.call(args.ctx, search, term, val, false, start));
        }
        else return val.indexOf(Values.toString(args.ctx, term), start);
    }
    @Expose public static int __lastIndexOf(Arguments args) {
        var val = passThis(args, "lastIndexOf");
        var term = args.get(0);
        var start = args.getInt(1);
        var search = Values.getMember(args.ctx, term, Symbol.get("Symbol.search"));

        if (search instanceof FunctionValue) {
            return (int)Values.toNumber(args.ctx, Values.call(args.ctx, search, term, val, true, start));
        }
        else return val.lastIndexOf(Values.toString(args.ctx, term), start);
    }

    @Expose public static boolean __includes(Arguments args) {
        return __indexOf(args) >= 0;
    }

    @Expose public static String __replace(Arguments args) {
        var val = passThis(args, "replace");
        var term = args.get(0);
        var replacement = args.get(1);
        var replace = Values.getMember(args.ctx, term, Symbol.get("Symbol.replace"));

        if (replace instanceof FunctionValue) {
            return Values.toString(args.ctx, Values.call(args.ctx, replace, term, val, replacement));
        }
        else return val.replaceFirst(Pattern.quote(Values.toString(args.ctx, term)), Values.toString(args.ctx, replacement));
    }
    @Expose public static String __replaceAll(Arguments args) {
        var val = passThis(args, "replaceAll");
        var term = args.get(0);
        var replacement = args.get(1);
        var replace = Values.getMember(args.ctx, term, Symbol.get("Symbol.replace"));

        if (replace instanceof FunctionValue) {
            return Values.toString(args.ctx, Values.call(args.ctx, replace, term, val, replacement));
        }
        else return val.replace(Values.toString(args.ctx, term), Values.toString(args.ctx, replacement));
    }

    @Expose public static ArrayValue __match(Arguments args) {
        var val = passThis(args, "match");
        var term = args.get(0);

        FunctionValue match;

        try {
            var _match = Values.getMember(args.ctx, term, Symbol.get("Symbol.match"));
            if (_match instanceof FunctionValue) match = (FunctionValue)_match;
            else if (args.ctx.hasNotNull(Environment.REGEX_CONSTR)) {
                var regex = Values.callNew(args.ctx, args.ctx.get(Environment.REGEX_CONSTR), Values.toString(args.ctx, term), "");
                _match = Values.getMember(args.ctx, regex, Symbol.get("Symbol.match"));
                if (_match instanceof FunctionValue) match = (FunctionValue)_match;
                else throw EngineException.ofError("Regular expressions don't support matching.");
            }
            else throw EngineException.ofError("Regular expressions not supported.");
        }
        catch (IllegalArgumentException e) { return new ArrayValue(args.ctx, ""); }

        var res = match.call(args.ctx, term, val);
        if (res instanceof ArrayValue) return (ArrayValue)res;
        else return new ArrayValue(args.ctx, "");
    }
    @Expose public static Object __matchAll(Arguments args) {
        var val = passThis(args, "matchAll");
        var term = args.get(0);

        FunctionValue match = null;
        
        try {
            var _match = Values.getMember(args.ctx, term, Symbol.get("Symbol.matchAll"));
            if (_match instanceof FunctionValue) match = (FunctionValue)_match;
        }
        catch (IllegalArgumentException e) { }

        if (match == null && args.ctx.hasNotNull(Environment.REGEX_CONSTR)) {
            var regex = Values.callNew(args.ctx, args.ctx.get(Environment.REGEX_CONSTR), Values.toString(args.ctx, term), "g");
            var _match = Values.getMember(args.ctx, regex, Symbol.get("Symbol.matchAll"));
            if (_match instanceof FunctionValue) match = (FunctionValue)_match;
            else throw EngineException.ofError("Regular expressions don't support matching.");
        }
        else throw EngineException.ofError("Regular expressions not supported.");

        return match.call(args.ctx, term, val);
    }

    @Expose public static ArrayValue __split(Arguments args) {
        var val = passThis(args, "split");
        var term = args.get(0);
        var lim = args.get(1);
        var sensible = args.getBoolean(2);

        if (lim != null) lim = Values.toNumber(args.ctx, lim);

        if (term != null && term != Values.NULL && !(term instanceof String)) {
            var replace = Values.getMember(args.ctx, term, Symbol.get("Symbol.replace"));
            if (replace instanceof FunctionValue) {
                var tmp = ((FunctionValue)replace).call(args.ctx, term, val, lim, sensible);

                if (tmp instanceof ArrayValue) {
                    var parts = new ArrayValue(((ArrayValue)tmp).size());
                    for (int i = 0; i < parts.size(); i++) parts.set(args.ctx, i, Values.toString(args.ctx, ((ArrayValue)tmp).get(i)));
                    return parts;
                }
            }
        }

        String[] parts;
        var pattern = Pattern.quote(Values.toString(args.ctx, term));

        if (lim == null) parts = val.split(pattern);
        else if ((double)lim < 1) return new ArrayValue();
        else if (sensible) parts = val.split(pattern, (int)(double)lim);
        else {
            var limit = (int)(double)lim;
            parts = val.split(pattern, limit + 1);
            ArrayValue res;

            if (parts.length > limit) res = new ArrayValue(limit);
            else res = new ArrayValue(parts.length);

            for (var i = 0; i < parts.length && i < limit; i++) res.set(args.ctx, i, parts[i]);

            return res;
        }

        var res = new ArrayValue(parts.length);
        var i = 0;

        for (; i < parts.length; i++) {
            if (lim != null && (double)lim <= i) break;
            res.set(args.ctx, i, parts[i]);
        }

        return res;
    }

    @Expose public static String __slice(Arguments args) {
        var self = passThis(args, "slice");
        var start = normalizeI(args.getInt(0), self.length(), false);
        var end = normalizeI(args.getInt(1, self.length()), self.length(), false);

        return __substring(new Arguments(args.ctx, self, start, end));
    }

    @Expose public static String __concat(Arguments args) {
        var res = new StringBuilder(passThis(args, "concat"));

        for (var el : args.convert(String.class)) res.append(el);

        return res.toString();
    }
    @Expose public static String __trim(Arguments args) {
        return passThis(args, "trim").trim();
    }
    @Expose public static String __trimStart(Arguments args) {
        return passThis(args, "trimStart").replaceAll("^\\s+", "");
    }
    @Expose public static String __trimEnd(Arguments args) {
        return passThis(args, "trimEnd").replaceAll("\\s+$", "");
    }

    @ExposeConstructor public static Object __constructor(Arguments args) {
        var val = args.getString(0, "");
        if (args.self instanceof ObjectValue) return new StringLib(val);
        else return val;
    }
    @Expose public static String __toString(Arguments args) {
        return passThis(args, "toString");
    }
    @Expose public static String __valueOf(Arguments args) {
        return passThis(args, "valueOf");
    }

    @Expose(target = ExposeTarget.STATIC)
    public static String __fromCharCode(Arguments args) {
        var val = args.convertInt();
        char[] arr = new char[val.length];
        for (var i = 0; i < val.length; i++) arr[i] = (char)val[i];
        return new String(arr);
    }
}
