package me.topchetoeu.jscript.permissions;

import java.util.ArrayList;

public class Permission {
    public final String[][] segments;

    private boolean matchSeg(String a, String b) {
        if (a.contains("**") || b.contains("**")) throw new IllegalArgumentException("A '**' segment may not contain other characters.");

        var segA = a.split("\\*", -1);
        var segB = b.split("\\*", -1);

        if (segA.length == 1 || segB.length == 1) {
            if (segA.length == 1 && segB.length == 1) return a.equals(b);
            else if (segA.length == 1) return matchSeg(b, a);
            else {
                if (!b.startsWith(segA[0]) || !b.endsWith(segA[segA.length - 1])) return false;

                int end = b.length() - segA[segA.length - 1].length();

                for (int i = 1, findI = 1; i < segA.length - 1; i++) {
                    findI = b.indexOf(segA[i], findI);
                    if (findI < 0 || findI + segA[i].length() > end) return false;
                }

                return true;
            }
        }

        String firstA = segA[0], firstB = segB[0];
        String lastA = segA[segA.length - 1], lastB = segB[segB.length - 1];

        if (!firstA.startsWith(firstB) && !firstB.startsWith(firstA)) return false;
        if (!lastA.endsWith(lastB) && !lastB.endsWith(lastA)) return false;

        return true;
    }
    private boolean matchArrs(String[] a, String[] b, int start, int end) {
        if (a.length != end - start) return false;
        if (a.length == 0) return true;
        if (start >= b.length || end > b.length) return false;
        if (start < 0 || end <= 0) return false;

        for (var i = start; i < end; i++) {
            if (!matchSeg(a[i - start], b[i])) return false;
        }

        return true;
    }
    private boolean matchFull(String[] a, String[] b) {
        return matchArrs(a, b, 0, b.length);
    }
    private boolean matchStart(String[] a, String[] b) {
        return matchArrs(a, b, 0, a.length);
    }
    private boolean matchEnd(String[] a, String[] b) {
        return matchArrs(a, b, b.length - a.length, b.length);
    }

    private int find(String[] query, String[] target, int start, int end) {
        var findI = 0;

        if (start < 0) start = 0;
        if (query.length == 0) return start;
        if (start != 0 && start >= target.length) return -1;

        for (var i = start; i < end; i++) {
            if (findI == query.length) return i - findI;
            else if (matchSeg(query[findI], target[i])) findI++;
            else {
                i -= findI;
                findI = 0;
            }
        }

        return -1;
    }

    public boolean match(Permission other) {
        var an = this.segments.length;
        var bn = other.segments.length;

        // We must have at least one segment, even if empty
        if (an == 0 || bn == 0) throw new IllegalArgumentException("Can't have a permission with 0 segments.");

        if (an == 1 || bn == 1) {
            // If both perms are one segment, we just match the segments themselves
            if (an == 1 && bn == 1) return matchFull(this.segments[0], other.segments[0]);
            else if (an == 1) return other.match(this);
            else {
                // If just the other perm is one segment, we neet to find all
                // the segments of this perm sequentially in the other segment.

                var seg = other.segments[0];
                // Here we check for the prefix and suffix
                if (!matchStart(this.segments[0], seg)) return false;
                if (!matchEnd(this.segments[this.segments.length - 1], seg)) return false;

                int end = seg.length - this.segments[this.segments.length - 1].length;

                // Here we go and look for the segments one by one, until one isn't found
                for (int i = 1, findI = 1; i < this.segments.length - 1; i++) {
                    findI = find(this.segments[i], seg, findI, end);
                    if (findI < 0) return false;
                }

                return true;
            }
        }

        // If both perms have more than one segment (a.k.a both have **),
        // we can ignore everything in the middle, as it will always match.
        // Instead, we check if the prefixes and suffixes match

        var firstA = this.segments[0];
        var firstB = other.segments[0];

        var lastA = this.segments[this.segments.length - 1];
        var lastB = other.segments[other.segments.length - 1];

        if (!matchStart(firstA, firstB) && !matchStart(firstB, firstA)) return false;
        if (!matchEnd(lastA, lastB) && !matchEnd(lastB, lastA)) return false;

        return true;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        var firstSeg = true;
        var firstEl = true;

        for (var seg : segments) {
            if (!firstSeg) {
                if (!firstEl) sb.append(".");
                sb.append("**");
            }
            firstSeg = false;
            for (var el : seg) {
                if (!firstEl) sb.append(".");
                sb.append(el);
                firstEl = false;
            }
        }

        return sb.toString();
    }

    public Permission(String raw) {
        var segs = raw.split("\\.");
        var curr = new ArrayList<String>();
        var res = new ArrayList<String[]>();

        for (var seg : segs) {
            if (seg.equals("**")) {
                res.add(curr.toArray(String[]::new));
                curr.clear();
            }
            else curr.add(seg);
        }
        res.add(curr.toArray(String[]::new));

        segments = res.toArray(String[][]::new);
    }

    public static boolean match(String a, String b) {
        return new Permission(a).match(new Permission(b));
    }
}
