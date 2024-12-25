package me.topchetoeu.jscript.common.parsing;

import java.util.ArrayList;
import java.util.Objects;

public abstract class Location implements Comparable<Location> {
	public static final Location INTERNAL = Location.of(new Filename("jscript", "native"), -1, -1);

	public abstract int line();
	public abstract int start();
	public abstract Filename filename();

	public final String toString() {
		var res = new ArrayList<String>();

		if (filename() != null) res.add(filename().toString());
		if (line() >= 0) res.add(line() + 1 + "");
		if (start() >= 0) res.add(start() + 1 + "");

		return String.join(":", res);
	}

	public final Location add(int n) {
		var self = this;

		return new Location() {
			@Override public Filename filename() { return self.filename(); }
			@Override public int start() { return self.start() + n; }
			@Override public int line() { return self.line(); }
		};
	}
	public final Location nextLine() {
		return changeLine(1);
	}
	public final Location changeLine(int offset) {
		var self = this;

		return new Location() {
			@Override public Filename filename() { return self.filename(); }
			@Override public int start() { return 0; }
			@Override public int line() { return self.line() + offset; }
		};
	}

	@Override public int hashCode() {
		return Objects.hash(line(), start(), filename());
	}
	@Override public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Location)) return false;
		var other = (Location)obj;

		if (!Objects.equals(this.start(), other.start())) return false;
		if (!Objects.equals(this.line(), other.line())) return false;
		if (!Objects.equals(this.filename(), other.filename())) return false;

		return true;
	}

	@Override public int compareTo(Location other) {
		int a = filename().toString().compareTo(other.filename().toString());
		int b = Integer.compare(line(), other.line());
		int c = Integer.compare(start(), other.start());

		if (a != 0) return a;
		if (b != 0) return b;

		return c;
	}

	public static Location of(Filename filename, int line, int start) {
		return new Location() {
			@Override public Filename filename() { return filename; }
			@Override public int start() { return start; }
			@Override public int line() { return line; }
		};
	}

	public static Location of(String raw) {
		var i0 = raw.lastIndexOf(':');
		if (i0 < 0) return Location.of(Filename.parse(raw), -1, -1);

		var i1 = raw.lastIndexOf(':', i0);
		if (i0 < 0) {
			try {
				return Location.of(Filename.parse(raw.substring(0, i0)), Integer.parseInt(raw.substring(i0 + 1)), -1);
			}
			catch (NumberFormatException e) {
				return Location.of(Filename.parse(raw), -1, -1);
			}
		}

		int start, line;

		try {
			start = Integer.parseInt(raw.substring(i1 + 1));
		}
		catch (NumberFormatException e) {
			return Location.of(Filename.parse(raw), -1, -1);
		}

		try {
			line = Integer.parseInt(raw.substring(i0 + 1, i1));
		}
		catch (NumberFormatException e) {
			return Location.of(Filename.parse(raw.substring(i1 + 1)), start, -1);
		}

		return Location.of(Filename.parse(raw.substring(0, i0)), start, line);
	}
}
