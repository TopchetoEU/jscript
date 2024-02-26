package me.topchetoeu.jscript.compilation.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.core.scope.LocalScopeRecord;

public class FunctionMap {
    public static class FunctionMapBuilder {
        private final TreeMap<Integer, Location> sourceMap = new TreeMap<>();
        private final HashMap<Location, BreakpointType> breakpoints = new HashMap<>();

        public Location toLocation(int pc) {
            return sourceMap.headMap(pc, true).firstEntry().getValue();
        }

        public FunctionMapBuilder setDebug(Location loc, BreakpointType type) {
            breakpoints.put(loc, type);
            return this;
        }
        public FunctionMapBuilder setLocation(int i, Location loc) {
            sourceMap.put(i, loc);
            return this;
        }
        public FunctionMapBuilder setLocationAndDebug(int i, Location loc, BreakpointType type) {
            setDebug(loc, type);
            setLocation(i, loc);
            return this;
        }

        public Location first() {
            return sourceMap.firstEntry().getValue();
        }
        public Location last() {
            return sourceMap.lastEntry().getValue();
        }

        public FunctionMap build(String[] localNames, String[] captureNames) {
            return new FunctionMap(sourceMap, breakpoints, localNames, captureNames);
        }
        public FunctionMap build(LocalScopeRecord scope) {
            return new FunctionMap(sourceMap, breakpoints, scope.locals(), scope.captures());
        }
        public FunctionMap build() {
            return new FunctionMap(sourceMap, breakpoints, new String[0], new String[0]);
        }

        private FunctionMapBuilder() { }
    }

    public static final FunctionMap EMPTY = new FunctionMap();

    private final HashMap<Location, BreakpointType> bps = new HashMap<>();
    private final TreeSet<Location> bpLocs = new TreeSet<>();

    private final TreeMap<Integer, Location> pcToLoc = new TreeMap<>();
    private final HashMap<Location, Integer> locToPc = new HashMap<>();

    public final String[] localNames, captureNames;

    public Location toLocation(int pc, boolean approxiamte) {
        var res = pcToLoc.get(pc);
        if (!approxiamte || res != null) return res;
        return pcToLoc.headMap(pc, true).lastEntry().getValue();
    }
    public Location toLocation(int pc) {
        return toLocation(pc, false);
    }

    public BreakpointType getBreakpoint(Location loc) {
        return bps.getOrDefault(loc, BreakpointType.NONE);
    }
    public Location correctBreakpoint(Location loc) {
        return bpLocs.ceiling(loc);
    }
    public SortedSet<Location> breakpoints() {
        return Collections.unmodifiableSortedSet(bpLocs);
    }

    public Location start() {
        return pcToLoc.firstEntry().getValue();
    }
    public Location end() {
        return pcToLoc.lastEntry().getValue();
    }

    public Integer toProgramCounter(Location loc) {
        return locToPc.get(loc);
    }

    public FunctionMap apply(SourceMap map) {
        var res = new FunctionMap();

        for (var el : new ArrayList<>(pcToLoc.entrySet())) {
            res.pcToLoc.put(el.getKey(), map.toCompiled(el.getValue()));
        }
        for (var el : new ArrayList<>(locToPc.entrySet())) {
            var mapped = map.toOriginal(el.getKey());
            res.locToPc.put(mapped, el.getValue());
        }

        return res;
    }

    public FunctionMap clone() {
        var res = new FunctionMap();
        res.pcToLoc.putAll(this.pcToLoc);
        res.locToPc.putAll(this.locToPc);
        return res;
    }

    public FunctionMap(Map<Integer, Location> map, Map<Location, BreakpointType> breakpoints, String[] localNames, String[] captureNames) {
        for (var el : map.entrySet()) {
            var pc = el.getKey();
            var loc = el.getValue();

            var a = pcToLoc.remove(pc);
            var b = locToPc.remove(loc);

            if (b != null) pcToLoc.remove(b);
            if (a != null) locToPc.remove(a);
        }
        for (var el : breakpoints.entrySet()) {
            if (el.getValue() == null || el.getValue() == BreakpointType.NONE) continue;
            bps.put(el.getKey(), el.getValue());
            bpLocs.add(el.getKey());
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