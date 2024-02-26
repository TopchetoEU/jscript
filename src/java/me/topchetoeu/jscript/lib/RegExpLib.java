package me.topchetoeu.jscript.lib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import me.topchetoeu.jscript.core.Context;
import me.topchetoeu.jscript.core.values.ArrayValue;
import me.topchetoeu.jscript.core.values.FunctionValue;
import me.topchetoeu.jscript.core.values.NativeWrapper;
import me.topchetoeu.jscript.core.values.ObjectValue;
import me.topchetoeu.jscript.core.values.Values;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeConstructor;
import me.topchetoeu.jscript.utils.interop.ExposeTarget;
import me.topchetoeu.jscript.utils.interop.ExposeType;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("RegExp")
public class RegExpLib {
    // I used Regex to analyze Regex
    private static final Pattern NAMED_PATTERN = Pattern.compile("\\(\\?<([^=!].*?)>", Pattern.DOTALL);
    private static final Pattern ESCAPE_PATTERN = Pattern.compile("[/\\-\\\\^$*+?.()|\\[\\]{}]");

    private Pattern pattern;
    private String[] namedGroups;
    private int flags;

    public int lastI = 0;
    public final String source;
    public final boolean hasIndices;
    public final boolean global;
    public final boolean sticky;

    @Expose(type = ExposeType.GETTER)
    public int __lastIndex() { return lastI; }
    @Expose(type = ExposeType.SETTER)
    public void __setLastIndex(Arguments args) { lastI = args.getInt(0); }
    @Expose(type = ExposeType.GETTER)
    public String __source() { return source; }

    @Expose(type = ExposeType.GETTER)
    public boolean __ignoreCase() { return (flags & Pattern.CASE_INSENSITIVE) != 0; }
    @Expose(type = ExposeType.GETTER)
    public boolean __multiline() { return (flags & Pattern.MULTILINE) != 0; }
    @Expose(type = ExposeType.GETTER)
    public boolean __unicode() { return (flags & Pattern.UNICODE_CHARACTER_CLASS) != 0; }
    @Expose(type = ExposeType.GETTER)
    public boolean __dotAll() { return (flags & Pattern.DOTALL) != 0; }
    @Expose(type = ExposeType.GETTER)
    public boolean __global() { return global; }
    @Expose(type = ExposeType.GETTER)
    public boolean __sticky() { return sticky; }
    @Expose(type = ExposeType.GETTER)
    public final String __flags() {
        String res = "";
        if (hasIndices) res += 'd';
        if (global) res += 'g';
        if (__ignoreCase()) res += 'i';
        if (__multiline()) res += 'm';
        if (__dotAll()) res += 's';
        if (__unicode()) res += 'u';
        if (sticky) res += 'y';
        return res;
    }

    @Expose public Object __exec(Arguments args) {
        var str = args.getString(0);
        var matcher = pattern.matcher(str);
        if (lastI > str.length() || !matcher.find(lastI) || sticky && matcher.start() != lastI) {
            lastI = 0;
            return Values.NULL;
        }
        if (sticky || global) {
            lastI = matcher.end();
            if (matcher.end() == matcher.start()) lastI++;
        }

        var obj = new ArrayValue();
        ObjectValue groups = null;

        for (var el : namedGroups) {
            if (groups == null) groups = new ObjectValue();
            try { groups.defineProperty(null, el, matcher.group(el)); }
            catch (IllegalArgumentException e) { }
        }


        for (int i = 0; i < matcher.groupCount() + 1; i++) {
            obj.set(null, i, matcher.group(i));
        }
        obj.defineProperty(null, "groups", groups);
        obj.defineProperty(null, "index", matcher.start());
        obj.defineProperty(null, "input", str);

        if (hasIndices) {
            var indices = new ArrayValue();
            for (int i = 0; i < matcher.groupCount() + 1; i++) {
                indices.set(null, i, new ArrayValue(null, matcher.start(i), matcher.end(i)));
            }
            var groupIndices = new ObjectValue();
            for (var el : namedGroups) {
                groupIndices.defineProperty(null, el, new ArrayValue(null, matcher.start(el), matcher.end(el)));
            }
            indices.defineProperty(null, "groups", groupIndices);
            obj.defineProperty(null, "indices", indices);
        }

        return obj;
    }

    @Expose public boolean __test(Arguments args) {
        return this.__exec(args) != Values.NULL;
    }

    @Expose("@@Symbol.match") public Object __match(Arguments args) {
        if (this.global) {
            var res = new ArrayValue();
            Object val;
            while ((val = this.__exec(args)) != Values.NULL) {
                res.set(args.ctx, res.size(), Values.getMember(args.ctx, val, 0));
            }
            lastI = 0;
            return res;
        }
        else {
            var res = this.__exec(args);
            if (!this.sticky) this.lastI = 0;
            return res;
        }
    }

    @Expose("@@Symbol.matchAll") public Object __matchAll(Arguments args) {
        var pattern = this.toGlobal();

        return Values.toJSIterator(args.ctx, new Iterator<Object>() {
            private Object val = null;
            private boolean updated = false;

            private void update() {
                if (!updated) val = pattern.__exec(args);
            }
            @Override public boolean hasNext() {
                update();
                return val != Values.NULL;
            }
            @Override public Object next() {
                update();
                updated = false;
                return val;
            }
        });
    }

    @Expose("@@Symbol.split") public ArrayValue __split(Arguments args) {
        var pattern = this.toGlobal();
        var target = args.getString(0);
        var hasLimit = args.get(1) != null;
        var lim = args.getInt(1);
        var sensible = args.getBoolean(2);

        Object match;
        int lastEnd = 0;
        var res = new ArrayValue();

        while ((match = pattern.__exec(args)) != Values.NULL) {
            var added = new ArrayList<String>();
            var arrMatch = (ArrayValue)match;
            int index = (int)Values.toNumber(args.ctx, Values.getMember(args.ctx, match, "index"));
            var matchVal = (String)arrMatch.get(0);

            if (index >= target.length()) break;

            if (matchVal.length() == 0 || index - lastEnd > 0) {
                added.add(target.substring(lastEnd, pattern.lastI));
                if (pattern.lastI < target.length()) {
                    for (var i = 1; i < arrMatch.size(); i++) added.add((String)arrMatch.get(i));
                }
            }
            else {
                for (var i = 1; i < arrMatch.size(); i++) added.add((String)arrMatch.get(i));
            }

            if (sensible) {
                if (hasLimit && res.size() + added.size() >= lim) break;
                else for (var i = 0; i < added.size(); i++) res.set(args.ctx, res.size(), added.get(i));
            }
            else {
                for (var i = 0; i < added.size(); i++) {
                    if (hasLimit && res.size() >= lim) return res;
                    else res.set(args.ctx, res.size(), added.get(i));
                }
            }
            lastEnd = pattern.lastI;
        }
        if (lastEnd < target.length()) {
            res.set(args.ctx, res.size(), target.substring(lastEnd));
        }
        return res;
    }

    @Expose("@@Symbol.replace") public String __replace(Arguments args) {
        var pattern = this.toIndexed();
        var target = args.getString(0);
        var replacement = args.get(1);
        Object match;
        var lastEnd = 0;
        var res = new StringBuilder();
    
        while ((match = pattern.__exec(args)) != Values.NULL) {
            var indices = (ArrayValue)((ArrayValue)Values.getMember(args.ctx, match, "indices")).get(0);
            var arrMatch = (ArrayValue)match;

            var start = ((Number)indices.get(0)).intValue();
            var end = ((Number)indices.get(1)).intValue();

            res.append(target.substring(lastEnd, start));
            if (replacement instanceof FunctionValue) {
                var callArgs = new Object[arrMatch.size() + 2];
                callArgs[0] = target.substring(start, end);
                arrMatch.copyTo(callArgs, 1, 1, arrMatch.size() - 1);
                callArgs[callArgs.length - 2] = start;
                callArgs[callArgs.length - 1] = target;
                res.append(Values.toString(args.ctx, ((FunctionValue)replacement).call(args.ctx, null, callArgs)));
            }
            else {
                res.append(Values.toString(args.ctx, replacement));
            }
            lastEnd = end;
            if (!pattern.global) break;
        }
        if (lastEnd < target.length()) {
            res.append(target.substring(lastEnd));
        }
        return res.toString();
    }

    // [Symbol.search](target, reverse, start) {
    //     const pattern: RegExp | undefined = new this.constructor(this, this.flags + "g") as RegExp;
    //     if (!reverse) {
    //         pattern.lastIndex = (start as any) | 0;
    //         const res = pattern.exec(target);
    //         if (res) return res.index;
    //         else return -1;
    //     }
    //     else {
    //         start ??= target.length;
    //         start |= 0;
    //         let res: RegExpResult | null = null;
    //         while (true) {
    //             const tmp = pattern.exec(target);
    //             if (tmp === null || tmp.index > start) break;
    //             res = tmp;
    //         }
    //         if (res && res.index <= start) return res.index;
    //         else return -1;
    //     }
    // },

    public RegExpLib toGlobal() {
        return new RegExpLib(pattern, namedGroups, flags, source, hasIndices, true, sticky);
    }
    public RegExpLib toIndexed() {
        return new RegExpLib(pattern, namedGroups, flags, source, true, global, sticky);
    }

    public String toString() {
        return "/" + source + "/" + __flags();
    }

    public RegExpLib(String pattern, String flags) {
        if (pattern == null || pattern.equals("")) pattern = "(?:)";
        if (flags == null || flags.equals("")) flags = "";

        this.flags = 0;
        this.hasIndices = flags.contains("d");
        this.global = flags.contains("g");
        this.sticky = flags.contains("y");
        this.source = pattern;

        if (flags.contains("i")) this.flags |= Pattern.CASE_INSENSITIVE;
        if (flags.contains("m")) this.flags |= Pattern.MULTILINE;
        if (flags.contains("s")) this.flags |= Pattern.DOTALL;
        if (flags.contains("u")) this.flags |= Pattern.UNICODE_CHARACTER_CLASS;

        if (pattern.equals("{(\\d+)}")) pattern = "\\{([0-9]+)\\}";
        this.pattern = Pattern.compile(pattern.replace("\\d", "[0-9]"), this.flags);

        var matcher = NAMED_PATTERN.matcher(source);
        var groups = new ArrayList<String>();

        while (matcher.find()) {
            if (!checkEscaped(source, matcher.start() - 1)) {
                groups.add(matcher.group(1));
            }
        }

        namedGroups = groups.toArray(String[]::new);
    }

    private RegExpLib(Pattern pattern, String[] namedGroups, int flags, String source, boolean hasIndices, boolean global, boolean sticky) {
        this.pattern = pattern;
        this.namedGroups = namedGroups;
        this.flags = flags;
        this.source = source;
        this.hasIndices = hasIndices;
        this.global = global;
        this.sticky = sticky;
    }
    public RegExpLib(String pattern) { this(pattern, null); }
    public RegExpLib() { this(null, null); }

    @ExposeConstructor
    public static RegExpLib __constructor(Arguments args) {
        return new RegExpLib(cleanupPattern(args.ctx, args.get(0)), cleanupFlags(args.ctx, args.get(1)));
    }
    @Expose(target = ExposeTarget.STATIC)
    public static RegExpLib __escape(Arguments args) {
        return escape(Values.toString(args.ctx, args.get(0)), cleanupFlags(args.ctx, args.get(1)));
    }

    private static String cleanupPattern(Context ctx, Object val) {
        if (val == null) return "(?:)";
        if (val instanceof RegExpLib) return ((RegExpLib)val).source;
        if (val instanceof NativeWrapper && ((NativeWrapper)val).wrapped instanceof RegExpLib) {
            return ((RegExpLib)((NativeWrapper)val).wrapped).source;
        }
        var res = Values.toString(ctx, val);
        if (res.equals("")) return "(?:)";
        return res;
    }
    private static String cleanupFlags(Context ctx, Object val) {
        if (val == null) return "";
        return Values.toString(ctx, val);
    }

    private static boolean checkEscaped(String s, int pos) {
        int n = 0;

        while (true) {
            if (pos <= 0) break;
            if (s.charAt(pos) != '\\') break;
            n++;
            pos--;
        }

        return (n % 2) != 0;
    }

    public static RegExpLib escape(String raw, String flags) {
        return new RegExpLib(ESCAPE_PATTERN.matcher(raw).replaceAll("\\\\$0"), flags);
    }
}
