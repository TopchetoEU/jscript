package me.topchetoeu.jscript.compilation.parsing;

import java.util.function.Predicate;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Location;

public class Source {
    public final Filename filename;
    public final String src;

    private int[] lineStarts;

    public Location loc(int offset) {
        return new SourceLocation(filename, lineStarts, offset);
    }
    public boolean is(int i, char c) {
        return i >= 0 && i < src.length() && src.charAt(i) == c;
    }
    public boolean is(int i, String src) {
        if (i < 0 || i + src.length() > size()) return false;

        for (int j = 0; j < src.length(); j++) {
            if (at(i + j) != src.charAt(j)) return false;
        }

        return true;
    }
    public boolean is(int i, Predicate<Character> predicate) {
        if (i < 0 || i >= src.length()) return false;
        return predicate.test(at(i));
    }
    public char at(int i) {
        return src.charAt(i);
    }
    public char at(int i, char defaultVal) {
        if (i < 0 || i >= src.length()) return defaultVal;
        else return src.charAt(i);
    }
    public int size() {
        return src.length();
    }
    public String slice(int start, int end) {
        return src.substring(start, end);
    }

    public Source(Filename filename, String src) {
        this.filename = filename;
        this.src = src;

        int n = 1;
        lineStarts = new int[16];
        lineStarts[0] = 0;

        for (int i = src.indexOf("\n"); i > 0; i = src.indexOf("\n", i + 1)) {
            if (n >= lineStarts.length) {
                var newArr = new int[lineStarts.length * 2];
                System.arraycopy(lineStarts, 0, newArr, 0, n);
                lineStarts = newArr;
            }

            lineStarts[n++] = i + 1;
        }

        var newArr = new int[n];
        System.arraycopy(lineStarts, 0, newArr, 0, n);
        lineStarts = newArr;
    }
}
