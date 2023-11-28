package me.topchetoeu.jscript.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.json.JSON;

public class SourceMap implements LocationMap {
    private final TreeMap<Location, Location> orgToSrc = new TreeMap<>();
    private final TreeMap<Location, Location> srcToOrg = new TreeMap<>();

    public Location toSource(Location loc) {
        return convert(loc, orgToSrc);
    }

    public Location toOriginal(Location loc) {
        return convert(loc, srcToOrg);
    }

    public static Location convert(Location loc, TreeMap<Location, Location> map) {
        if (map.containsKey(loc)) return loc;

        var srcA = map.floorKey(loc);
        return srcA == null ? loc : srcA;
    }

    public void chain(LocationMap map) {
        for (var key : orgToSrc.keySet()) {
            orgToSrc.put(key, map.toSource(key));
        }
        for (var key : srcToOrg.keySet()) {
            srcToOrg.put(map.toOriginal(key), key);
        }
    }

    public SourceMap clone() {
        var res = new SourceMap();
        res.orgToSrc.putAll(this.orgToSrc);
        res.srcToOrg.putAll(this.srcToOrg);
        return res;
    }

    public void split(Map<Filename, TreeMap<Location, Location>> maps) {
        for (var el : orgToSrc.entrySet()) {
            var map = maps.get(el.getKey().filename());
            if (map == null) maps.put(el.getKey().filename(), map = new TreeMap<>());
            map.put(el.getKey(), el.getValue());

            map = maps.get(el.getValue().filename());
            if (map == null) maps.put(el.getValue().filename(), map = new TreeMap<>());
            map.put(el.getValue(), el.getKey());
        }
    }

    public static SourceMap parse(String raw) {
        var res = new SourceMap();
        var json = JSON.parse(null, raw).map();
        var sources = json.list("sources").stream().map(v -> v.string()).toArray(String[]::new);
        var dstFilename = Filename.parse(json.string("file"));
        var mapping = VLQ.decodeMapping(json.string("mappings"));

        var srcI = 0;
        var srcRow = 0;
        var srcCol = 0;

        for (var dstRow = 0; dstRow < mapping.length; dstRow++) {
            var dstCol = 0;

            for (var rawSeg : mapping[dstRow]) {
                dstCol += rawSeg.length > 0 ? rawSeg[0] : 0;
                srcI += rawSeg.length > 1 ? rawSeg[1] : 0;
                srcRow += rawSeg.length > 2 ? rawSeg[2] : 0;
                srcCol += rawSeg.length > 3 ? rawSeg[3] : 0;

                var src = new Location(srcRow + 1, srcCol + 1, Filename.parse(sources[srcI]));
                var dst = new Location(dstRow + 1, dstCol + 1, dstFilename);

                res.orgToSrc.put(src, dst);
                res.srcToOrg.put(dst, src);
            }
        }

        return res;
    }

    public static SourceMap chain(SourceMap ...maps) {
        if (maps.length == 0) return null;
        var res = maps[0];

        for (var i = 1; i < maps.length; i++) res.chain(maps[i]);

        return res;
    }
}
