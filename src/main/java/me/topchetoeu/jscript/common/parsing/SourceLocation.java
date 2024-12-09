package me.topchetoeu.jscript.common.parsing;

import java.util.Objects;

public class SourceLocation extends Location {
	private int[] lineStarts;
	private int line;
	private int start;
	private final Filename filename;
	private final int offset;

	private void update() {
		if (lineStarts == null) return;

		int a = 0;
		int b = lineStarts.length;

		while (true) {
			if (a + 1 >= b) break;
			var mid = -((-a - b) >> 1);
			var el = lineStarts[mid];

			if (el < offset) a = mid;
			else if (el > offset) b = mid;
			else {
				this.line = mid;
				this.start = 0;
				this.lineStarts = null;
				return;
			}
		}

		this.line = a;
		this.start = offset - lineStarts[a];
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

	@Override public int hashCode() {
		return Objects.hash(offset);
	}
	@Override public int compareTo(Location other) {
		if (other instanceof SourceLocation srcLoc) return Integer.compare(offset, srcLoc.offset);
		else return super.compareTo(other);
	}
	@Override public boolean equals(Object obj) {
		if (obj instanceof SourceLocation other) return this.offset == other.offset;
		else return super.equals(obj);
	}

	public SourceLocation(Filename filename, int[] lineStarts, int offset) {
		this.filename = filename;
		this.lineStarts = lineStarts;
		this.offset = offset;
	}
}
