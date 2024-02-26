package me.topchetoeu.jscript.compilation.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.VLQ;
import me.topchetoeu.jscript.common.json.JSON;

public class SourceMap {
    private final TreeMap<Long, Long> origToComp = new TreeMap<>();
    private final TreeMap<Long, Long> compToOrig = new TreeMap<>();

    public Location toCompiled(Location loc) { return convert(loc, origToComp); }
    public Location toOriginal(Location loc) { return convert(loc, compToOrig); }

    private void add(long orig, long comp) {
        var a = origToComp.remove(orig);
        var b = compToOrig.remove(comp);

        if (b != null) origToComp.remove(b);
        if (a != null) compToOrig.remove(a);

        origToComp.put(orig, comp);
        compToOrig.put(comp, orig);
    }

    public SourceMap apply(SourceMap map) {
        var res = new SourceMap();

        for (var el : new ArrayList<>(origToComp.entrySet())) {
            var mapped = convert(el.getValue(), map.origToComp);
            res.origToComp.put(el.getKey(), mapped);
        }
        for (var el : new ArrayList<>(compToOrig.entrySet())) {
            var mapped = convert(el.getKey(), map.compToOrig);
            res.compToOrig.put(mapped, el.getValue());
            res.add(el.getValue(), mapped);
        }

        return res;
    }

    public SourceMap clone() {
        var res = new SourceMap();
        res.origToComp.putAll(this.origToComp);
        res.compToOrig.putAll(this.compToOrig);
        return res;
    }

    public static SourceMap parse(String raw) {
        var mapping = VLQ.decodeMapping(raw);
        var res = new SourceMap();

        var compRow = 0l;
        var compCol = 0l;

        for (var origRow = 0; origRow < mapping.length; origRow++) {
            var origCol = 0;

            for (var rawSeg : mapping[origRow]) {
                if (rawSeg.length > 1 && rawSeg[1] != 0) throw new IllegalArgumentException("Source mapping is to more than one files.");
                origCol += rawSeg.length > 0 ? rawSeg[0] : 0;
                compRow += rawSeg.length > 2 ? rawSeg[2] : 0;
                compCol += rawSeg.length > 3 ? rawSeg[3] : 0;

                var compPacked = ((long)compRow << 32) | compCol;
                var origPacked = ((long)origRow << 32) | origCol;

                res.add(origPacked, compPacked);
            }
        }

        return res;
    }
    public static List<String> getSources(String raw) {
        var json = JSON.parse(null, raw).map();
        return json
            .list("sourcesContent")
            .stream()
            .map(v -> v.string())
            .collect(Collectors.toList());
    }

    public static SourceMap chain(SourceMap ...maps) {
        if (maps.length == 0) return null;
        var res = maps[0];

        for (var i = 1; i < maps.length; i++) res = res.apply(maps[i]);

        return res;
    }

    private static Long convert(long packed, TreeMap<Long, Long> map) {
        if (map.containsKey(packed)) return map.get(packed);
        var key = map.floorKey(packed);
        if (key == null) return null;
        else return map.get(key);
    }

    private static Location convert(Location loc, TreeMap<Long, Long> map) {
        var packed = ((loc.line() - 1l) << 32) | (loc.start() - 1);
        var resPacked = convert(packed, map);

        if (resPacked == null) return null;
        else return new Location((int)(resPacked >> 32) + 1, (int)(resPacked & 0xFFFF) + 1, loc.filename());
    }
}
