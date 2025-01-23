package me.topchetoeu.j2s.compilation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.topchetoeu.j2s.common.Filename;
import me.topchetoeu.j2s.common.FunctionMap;
import me.topchetoeu.j2s.common.Location;
import me.topchetoeu.j2s.common.Instruction.BreakpointType;

public final class CompilationFunctionMap implements FunctionMap {
	public static class FunctionMapBuilder {
		private Location first, last;
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
			if (first == null || first.compareTo(loc) > 0) first = loc;
			if (last == null || last.compareTo(loc) < 0) last = loc;

			sourceMap.put(i, loc);
			return this;
		}
		public FunctionMapBuilder setLocationAndDebug(int i, Location loc, BreakpointType type) {
			setDebug(loc, type);
			setLocation(i, loc);
			return this;
		}

		public Location first() {
			return first;
		}
		public Location last() {
			return last;
		}

		public FunctionMapBuilder map(Function<Location, Location> mapper) {
			var newSourceMaps = new HashMap<Integer, Location>();
			var newBreakpoints = new HashMap<Location, BreakpointType>();

			for (var key : sourceMap.keySet()) {
				var mapped = mapper.apply(sourceMap.get(key));
				if (mapped == null) continue;
				newSourceMaps.put(key, mapped);
			}
			for (var key : breakpoints.keySet()) {
				var mapped = mapper.apply(key);
				if (mapped == null) continue;
				newBreakpoints.put(mapped, breakpoints.get(key));
			}

			sourceMap.clear();
			sourceMap.putAll(newSourceMaps);

			breakpoints.clear();
			breakpoints.putAll(newBreakpoints);

			return this;
		}

		public CompilationFunctionMap build(String[] localNames, String[] capturableNames, String[] captureNames) {
			return new CompilationFunctionMap(sourceMap, breakpoints, first, last, localNames, capturableNames, captureNames);
		}
		public CompilationFunctionMap build() {
			return new CompilationFunctionMap(sourceMap, breakpoints, first, last, new String[0], new String[0], new String[0]);
		}

		private FunctionMapBuilder() { }
	}

	public static final CompilationFunctionMap EMPTY = new CompilationFunctionMap();

	private final HashMap<Integer, BreakpointType> bps = new HashMap<>();
	private final HashMap<Filename, TreeSet<Location>> bpLocs = new HashMap<>();

	private final TreeMap<Integer, Location> pcToLoc = new TreeMap<>();

	public final String[] localNames, capturableNames, captureNames;
	public final Location first, last;

	@Override public Location toLocation(int pc, boolean approximate) {
		if (pcToLoc.size() == 0 || pc < 0 || pc > pcToLoc.lastKey()) return null;
		var res = pcToLoc.get(pc);
		if (!approximate || res != null) return res;
		var entry = pcToLoc.headMap(pc, true).lastEntry();
		if (entry == null) return null;
		else return entry.getValue();
	}
	@Override public Location toLocation(int pc) {
		return toLocation(pc, false);
	}

	@Override public BreakpointType getBreakpoint(int pc) {
		return bps.getOrDefault(pc, BreakpointType.NONE);
	}
	@Override public Location correctBreakpoint(Location loc) {
		var set = bpLocs.get(loc.filename());
		if (set == null) return null;
		else return set.ceiling(loc);
	}
	@Override public List<Location> correctBreakpoint(Pattern filename, int line, int column) {
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
	@Override public List<Location> breakpoints(Location start, Location end) {
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

	@Override public Location first() {
		return first;
	}
	@Override public Location last() {
		return last;
	}

	@Override public String[] capturableNames() {
		return capturableNames;
	}
	@Override public String[] captureNames() {
		return captureNames;
	}
	@Override public String[] localNames() {
		return localNames;
	}

	public CompilationFunctionMap clone() {
		var res = new CompilationFunctionMap(new HashMap<>(), new HashMap<>(), first, last, localNames, capturableNames, captureNames);
		res.pcToLoc.putAll(this.pcToLoc);
		res.bps.putAll(bps);
		res.bpLocs.putAll(bpLocs);
		res.pcToLoc.putAll(pcToLoc);
		return res;
	}

	public CompilationFunctionMap(Map<Integer, Location> map, Map<Location, BreakpointType> breakpoints, Location first, Location last, String[] localNames, String[] capturableNames, String[] captureNames) {
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
		this.capturableNames = capturableNames;

		this.first = first;
		this.last = last;
	}
	private CompilationFunctionMap() {
		localNames = new String[0];
		captureNames = new String[0];
		capturableNames = new String[0];
		first = null;
		last = null;
	}

	public static FunctionMapBuilder builder() {
		return new FunctionMapBuilder();
	}
}