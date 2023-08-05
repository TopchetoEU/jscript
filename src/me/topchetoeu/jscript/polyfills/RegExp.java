package me.topchetoeu.jscript.polyfills;

import java.util.ArrayList;
import java.util.regex.Pattern;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.NativeWrapper;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeGetter;
import me.topchetoeu.jscript.interop.NativeSetter;

public class RegExp {
    // I used Regex to analyze Regex
    private static final Pattern NAMED_PATTERN = Pattern.compile("\\(\\?<([^=!].*?)>", Pattern.DOTALL);
    private static final Pattern ESCAPE_PATTERN = Pattern.compile("[/\\-\\\\^$*+?.()|\\[\\]{}]");

    private static String cleanupPattern(CallContext ctx, Object val) throws InterruptedException {
        if (val == null) return "(?:)";
        if (val instanceof RegExp) return ((RegExp)val).source;
        if (val instanceof NativeWrapper && ((NativeWrapper)val).wrapped instanceof RegExp) {
            return ((RegExp)((NativeWrapper)val).wrapped).source;
        }
        var res = Values.toString(ctx, val);
        if (res.equals("")) return "(?:)";
        return res;
    }
    private static String cleanupFlags(CallContext ctx, Object val) throws InterruptedException {
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
    public static RegExp escape(CallContext ctx, Object raw, Object flags) throws InterruptedException {
        return escape(Values.toString(ctx, raw), cleanupFlags(ctx, flags));
    }
    public static RegExp escape(String raw, String flags) {
        return new RegExp(ESCAPE_PATTERN.matcher(raw).replaceAll("\\\\$0"), flags);
    }

    private Pattern pattern;
    private String[] namedGroups;
    private int flags;
    private int lastI = 0;

    @Native
    public final String source;
    @Native
    public final boolean hasIndices;
    @Native
    public final boolean global;
    @Native
    public final boolean sticky;

    @NativeGetter("ignoreCase")
    public boolean ignoreCase() { return (flags & Pattern.CASE_INSENSITIVE) != 0; }
    @NativeGetter("multiline")
    public boolean multiline() { return (flags & Pattern.MULTILINE) != 0; }
    @NativeGetter("unicode")
    public boolean unicode() { return (flags & Pattern.UNICODE_CHARACTER_CLASS) != 0; }
    @NativeGetter("unicode")
    public boolean dotAll() { return (flags & Pattern.DOTALL) != 0; }

    @NativeGetter("lastIndex")
    public int lastIndex() { return lastI; }
    @NativeSetter("lastIndex")
    public void setLastIndex(CallContext ctx, Object i) throws InterruptedException {
        lastI = (int)Values.toNumber(ctx, i);
    }
    public void setLastIndex(int i) {
        lastI = i;
    }

    @NativeGetter("flags")
    public final String flags() {
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

    @Native
    public Object exec(CallContext ctx, Object str) throws InterruptedException {
        return exec(Values.toString(ctx, str));
    }
    public Object exec(String str) {
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
                groups.defineProperty(el, matcher.group(el));
            }
            catch (IllegalArgumentException e) { }
        }
        if (groups.values.size() == 0) groups = null;


        for (int i = 0; i < matcher.groupCount() + 1; i++) {
            obj.set(i, matcher.group(i));
        }
        obj.defineProperty("groups", groups);
        obj.defineProperty("index", matcher.start());
        obj.defineProperty("input", str);

        if (hasIndices) {
            var indices = new ArrayValue();
            for (int i = 0; i < matcher.groupCount() + 1; i++) {
                indices.set(i, new ArrayValue(matcher.start(i), matcher.end(i)));
            }
            var groupIndices = new ObjectValue();
            for (var el : namedGroups) {
                groupIndices.defineProperty(el, new ArrayValue(matcher.start(el), matcher.end(el)));
            }
            indices.defineProperty("groups", groupIndices);
            obj.defineProperty("indices", indices);
        }

        return obj;
    }

    @Native
    public RegExp(CallContext ctx, Object pattern, Object flags) throws InterruptedException {
        this(cleanupPattern(ctx, pattern), cleanupFlags(ctx, flags));
    }
    public RegExp(String pattern, String flags) {
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

    public RegExp(String pattern) { this(pattern, null); }
    public RegExp() { this(null, null); }
}
