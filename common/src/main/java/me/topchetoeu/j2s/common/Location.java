package me.topchetoeu.j2s.common;

import java.util.ArrayList;
import java.util.Objects;

public abstract class Location implements Comparable<Location> {
	public static final Location INTERNAL = Location.of(new Filename(Metadata.name(), "native"), -1, -1);

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
		return nextLine(1);
	}
	public final Location nextLine(int offset) {
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
		int a;
		var filenameA = filename();
		var filenameB = other.filename();
		if (filenameB == null && filenameA == null) a = 0;
		else if (filenameA == null) a = -1;
		else if (filenameB == null) a = 1;
		else a = filenameA.toString().compareTo(filenameB.toString());

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
		var i1 = raw.lastIndexOf(':');
		if (i1 < 0) return Location.of(Filename.parse(raw), -1, -1);

		var i0 = raw.substring(0, i1).lastIndexOf(':', i1);
		if (i0 < 0) {
			try {
				return Location.of(null, Integer.parseInt(raw.substring(0, i1)) - 1, Integer.parseInt(raw.substring(i1 + 1)) - 1);
			}
			catch (NumberFormatException e) {}

			try {
				return Location.of(Filename.parse(raw.substring(0, i1)), Integer.parseInt(raw.substring(i1 + 1)) - 1, -1);
			}
			catch (NumberFormatException e) {}

			return Location.of(Filename.parse(raw), -1, -1);
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
			return Location.of(Filename.parse(raw.substring(0, i1)), start - 1, -1);
		}

		return Location.of(Filename.parse(raw.substring(0, i0)), line - 1, start - 1);
	}
}
