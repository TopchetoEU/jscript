package me.topchetoeu.j2s.common;

import java.util.Arrays;
import java.util.regex.Pattern;

import me.topchetoeu.j2s.common.Instruction.BreakpointType;

public interface FunctionMap {
	public static final FunctionMap EMPTY = new FunctionMap() {
		@Override public Location first() {
			return null;
		}
		@Override public Location last() {
			return null;
		}

		@Override public Location toLocation(int i, boolean approximate) {
			return null;
		}

		@Override public BreakpointType getBreakpoint(int i) {
			return BreakpointType.NONE;
		}
		@Override public Iterable<Location> breakpoints(Location start, Location end) {
			return Arrays.asList();
		}

		@Override public Location correctBreakpoint(Location i) {
			return null;
		}
		@Override public Iterable<Location> correctBreakpoint(Pattern filename, int line, int column) {
			return Arrays.asList();
		}

		@Override public String[] localNames() {
			return null;
		}
		@Override public String[] capturableNames() {
			return null;
		}
		@Override public String[] captureNames() {
			return null;
		}
	};

	Location first();
	Location last();

	Location toLocation(int i, boolean approximate);
	default Location toLocation(int i) {
		return toLocation(i, false);
	}

	BreakpointType getBreakpoint(int i);
	Location correctBreakpoint(Location i);
	Iterable<Location> correctBreakpoint(Pattern filename, int line, int column);
	Iterable<Location> breakpoints(Location start, Location end);

	String[] localNames();
	String[] capturableNames();
	String[] captureNames();
}
