package me.topchetoeu.jscript.lib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.NativeWrapper;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeGetter;

public class RegExpLib {
    // I used Regex to analyze Regex
    private static final Pattern NAMED_PATTERN = Pattern.compile("\\(\\?<([^=!].*?)>", Pattern.DOTALL);
    private static final Pattern ESCAPE_PATTERN = Pattern.compile("[/\\-\\\\^$*+?.()|\\[\\]{}]");

    private static String cleanupPattern(Context ctx, Object val) throws InterruptedException {
        if (val == null) return "(?:)";
        if (val instanceof RegExpLib) return ((RegExpLib)val).source;
        if (val instanceof NativeWrapper && ((NativeWrapper)val).wrapped instanceof RegExpLib) {
            return ((RegExpLib)((NativeWrapper)val).wrapped).source;
        }
        var res = Values.toString(ctx, val);
        if (res.equals("")) return "(?:)";
        return res;
    }
    private static String cleanupFlags(Context ctx, Object val) throws InterruptedException {
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

    @Native
    public static RegExpLib escape(Context ctx, Object raw, Object flags) throws InterruptedException {
        return escape(Values.toString(ctx, raw), cleanupFlags(ctx, flags));
    }
    public static RegExpLib escape(String raw, String flags) {
        return new RegExpLib(ESCAPE_PATTERN.matcher(raw).replaceAll("\\\\$0"), flags);
    }

    private Pattern pattern;
    private String[] namedGroups;
    private int flags;
    
    @Native public int lastI = 0;
    @Native public final String source;
    @Native public final boolean hasIndices;
    @Native public final boolean global;
    @Native public final boolean sticky;
    @Native("@@Symbol.typeName") public final String name = "RegExp";

    @NativeGetter public boolean ignoreCase() { return (flags & Pattern.CASE_INSENSITIVE) != 0; }
    @NativeGetter public boolean multiline() { return (flags & Pattern.MULTILINE) != 0; }
    @NativeGetter public boolean unicode() { return (flags & Pattern.UNICODE_CHARACTER_CLASS) != 0; }
    @NativeGetter public boolean dotAll() { return (flags & Pattern.DOTALL) != 0; }

    @NativeGetter("flags") public final String flags() {
        String res = "";
        if (hasIndices) res += 'd';
        if (global) res += 'g';
        if (ignoreCase()) res += 'i';
        if (multiline()) res += 'm';
        if (dotAll()) res += 's';
        if (unicode()) res += 'u';
        if (sticky) res += 'y';
        return res;
    }

    
    @Native public Object exec(String str) {
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
        var groups = new ObjectValue();

        for (var el : namedGroups) {
            try {
                groups.defineProperty(null, el, matcher.group(el));
            }
            catch (IllegalArgumentException e) { }
        }
        if (groups.values.size() == 0) groups = null;


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

    @Native public boolean test(String str) {
        return this.exec(str) != Values.NULL;
    }
    @Native public String toString() {
        return "/" + source + "/" + flags();
    }

    @Native("@@Symvol.match") public Object match(Context ctx, String target) throws InterruptedException {
        if (this.global) {
            var res = new ArrayValue();
            Object val;
            while ((val = this.exec(target)) != Values.NULL) {
                res.set(ctx, res.size(), Values.getMember(ctx, val, 0));
            }
            lastI = 0;
            return res;
        }
        else {
            var res = this.exec(target);
            if (!this.sticky) this.lastI = 0;
            return res;
        }
    }

    @Native("@@Symvol.matchAll") public Object matchAll(Context ctx, String target) throws InterruptedException {
        var pattern = new RegExpLib(this.source, this.flags() + "g");

        return Values.fromJavaIterator(ctx, new Iterator<Object>() {
            private Object val = null;
            private boolean updated = false;

            private void update() {
                if (!updated) val = pattern.exec(target);
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
    
    @Native("@@Symvol.split") public ArrayValue split(Context ctx, String target, Object limit, boolean sensible) throws InterruptedException {
        var pattern = new RegExpLib(this.source, this.flags() + "g");
        Object match;
        int lastEnd = 0;
        var res = new ArrayValue();
        var lim = limit == null ? 0 : Values.toNumber(ctx, limit);

        while ((match = pattern.exec(target)) != Values.NULL) {
            var added = new ArrayList<String>();
            var arrMatch = (ArrayValue)match;
            int index = (int)Values.toNumber(ctx, Values.getMember(ctx, match, "index"));
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
                if (limit != null && res.size() + added.size() >= lim) break;
                else for (var i = 0; i < added.size(); i++) res.set(ctx, res.size(), added.get(i));
            }
            else {
                for (var i = 0; i < added.size(); i++) {
                    if (limit != null && res.size() >= lim) return res;
                    else res.set(ctx, res.size(), added.get(i));
                }
            }
            lastEnd = pattern.lastI;
        }
        if (lastEnd < target.length()) {
            res.set(ctx, res.size(), target.substring(lastEnd));
        }
        return res;
    }
    // [Symbol.replace](target, replacement) {
    //     const pattern = new this.constructor(this, this.flags + "d") as RegExp;
    //     let match: RegExpResult | null;
    //     let lastEnd = 0;
    //     const res: string[] = [];
    //     // log(pattern.toString());
    //     while ((match = pattern.exec(target)) !== null) {
    //         const indices = match.indices![0];
    //         res.push(target.substring(lastEnd, indices[0]));
    //         if (replacement instanceof Function) {
    //             res.push(replacement(target.substring(indices[0], indices[1]), ...match.slice(1), indices[0], target));
    //         }
    //         else {
    //             res.push(replacement);
    //         }
    //         lastEnd = indices[1];
    //         if (!pattern.global) break;
    //     }
    //     if (lastEnd < target.length) {
    //         res.push(target.substring(lastEnd));
    //     }
    //     return res.join('');
    // },
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
    @Native public RegExpLib(Context ctx, Object pattern, Object flags) throws InterruptedException {
        this(cleanupPattern(ctx, pattern), cleanupFlags(ctx, flags));
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

    public RegExpLib(String pattern) { this(pattern, null); }
    public RegExpLib() { this(null, null); }
}
