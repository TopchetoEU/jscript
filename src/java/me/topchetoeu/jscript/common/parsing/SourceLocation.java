package me.topchetoeu.jscript.common.parsing;

public class SourceLocation extends Location {
    private int[] lineStarts;
    private int line;
    private int start;
    private final Filename filename;
    private final int offset;

    private void update() {
        if (lineStarts == null) return;

        int start = 0;
        int end = lineStarts.length - 1;

        while (true) {
            if (start + 1 >= end) break;
            var mid = -((-start - end) >> 1);
            var el = lineStarts[mid];

            if (el < offset) start = mid;
            else if (el > offset) end = mid;
            else {
                this.line = mid;
                this.start = 0;
                this.lineStarts = null;
                return;
            }
        }

        this.line = start;
        this.start = offset - lineStarts[start];
        this.lineStarts = null;
        return;
    }

    @Override public Filename filename() { return filename; }
    @Override public int line() {
        update();
        return line;
    }
    @Override public int start() {
        update();
        return start;
    }

    public SourceLocation(Filename filename, int[] lineStarts, int offset) {
        this.filename = filename;
        this.lineStarts = lineStarts;
        this.offset = offset;
    }
}
