package me.topchetoeu.jscript.common.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Filename;
import me.topchetoeu.jscript.common.parsing.Location;

public class FunctionMap {
	public static class FunctionMapBuilder {
		private final TreeMap<Integer, Location> sourceMap = new TreeMap<>();
		private final HashMap<Location, BreakpointType> breakpoints = new HashMap<>();

		public Location toLocation(int pc) {
			return sourceMap.headMap(pc, true).firstEntry().getValue();
		}

		public FunctionMapBuilder setDebug(Location loc, BreakpointType type) {
			if (loc == null || type == null || type == BreakpointType.NONE) return this;
			breakpoints.put(loc, type);
			return this;
		}
		public FunctionMapBuilder setLocation(int i, Location loc) {
			if (loc == null || i < 0) return this;
			sourceMap.put(i, loc);
			return this;
		}
		public FunctionMapBuilder setLocationAndDebug(int i, Location loc, BreakpointType type) {
			setDebug(loc, type);
			setLocation(i, loc);
			return this;
		}

		public Location first() {
			if (sourceMap.size() == 0) return null;
			return sourceMap.firstEntry().getValue();
		}
		public Location last() {
			if (sourceMap.size() == 0) return null;
			return sourceMap.lastEntry().getValue();
		}

		public FunctionMap build(String[] localNames, String[] captureNames) {
			return new FunctionMap(sourceMap, breakpoints, localNames, captureNames);
		}
		public FunctionMap build() {
			return new FunctionMap(sourceMap, breakpoints, new String[0], new String[0]);
		}

		private FunctionMapBuilder() { }
	}

	public static final FunctionMap EMPTY = new FunctionMap();

	private final HashMap<Integer, BreakpointType> bps = new HashMap<>();
	private final HashMap<Filename, TreeSet<Location>> bpLocs = new HashMap<>();

	private final TreeMap<Integer, Location> pcToLoc = new TreeMap<>();

	public final String[] localNames, captureNames;

	public Location toLocation(int pc, boolean approxiamte) {
		if (pcToLoc.size() == 0 || pc < 0 || pc > pcToLoc.lastKey()) return null;
		var res = pcToLoc.get(pc);
		if (!approxiamte || res != null) return res;
		var entry = pcToLoc.headMap(pc, true).lastEntry();
		if (entry == null) return null;
		else return entry.getValue();
	}
	public Location toLocation(int pc) {
		return toLocation(pc, false);
	}

	public BreakpointType getBreakpoint(int pc) {
		return bps.getOrDefault(pc, BreakpointType.NONE);
	}
	public Location correctBreakpoint(Location loc) {
		var set = bpLocs.get(loc.filename());
		if (set == null) return null;
		else return set.ceiling(loc);
	}
	public List<Location> correctBreakpoint(Pattern filename, int line, int column) {
		var candidates = new HashMap<Filename, TreeSet<Location>>();

		for (var name : bpLocs.keySet()) {
			if (filename.matcher(name.toString()).matches()) {
				candidates.put(name, bpLocs.get(name));
			}
		}

		var res = new ArrayList<Location>(candidates.size());
		for (var candidate : candidates.entrySet()) {
			var val = correctBreakpoint(Location.of(candidate.getKey(), line, column));
			if (val == null) continue;
			res.add(val);
		}

		return res;
	}
	public List<Location> breakpoints(Location start, Location end) {
		if (!Objects.equals(start.filename(), end.filename())) return Arrays.asList();
		NavigableSet<Location> set = bpLocs.get(start.filename());
		if (set == null) return Arrays.asList();

		if (start != null) set = set.tailSet(start, true);
		if (end != null) set = set.headSet(end, true);

		return set.stream().collect(Collectors.toList());
	}

	public Location start() {
		if (pcToLoc.size() == 0) return null;
		return pcToLoc.firstEntry().getValue();
	}
	public Location end() {
		if (pcToLoc.size() == 0) return null;
		return pcToLoc.lastEntry().getValue();
	}

	public FunctionMap clone() {
		var res = new FunctionMap(new HashMap<>(), new HashMap<>(), localNames, captureNames);
		res.pcToLoc.putAll(this.pcToLoc);
		res.bps.putAll(bps);
		res.bpLocs.putAll(bpLocs);
		res.pcToLoc.putAll(pcToLoc);
		return res;
	}

	public FunctionMap(Map<Integer, Location> map, Map<Location, BreakpointType> breakpoints, String[] localNames, String[] captureNames) {
		var locToPc = new HashMap<Location, Integer>();

		for (var el : map.entrySet()) {
			pcToLoc.put(el.getKey(), el.getValue());
			locToPc.putIfAbsent(el.getValue(), el.getKey());
		}

		for (var el : breakpoints.entrySet()) {
			if (el.getValue() == null || el.getValue() == BreakpointType.NONE) continue;
			bps.put(locToPc.get(el.getKey()), el.getValue());

			if (!bpLocs.containsKey(el.getKey().filename())) bpLocs.put(el.getKey().filename(), new TreeSet<>());
			bpLocs.get(el.getKey().filename()).add(el.getKey());
		}

		this.localNames = localNames;
		this.captureNames = captureNames;
	}
	private FunctionMap() {
		localNames = new String[0];
		captureNames = new String[0];
	}

	public static FunctionMapBuilder builder() {
		return new FunctionMapBuilder();
	}
}