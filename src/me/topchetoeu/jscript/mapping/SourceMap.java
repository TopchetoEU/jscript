package me.topchetoeu.jscript.mapping;

import java.util.TreeMap;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.json.JSON;

public class SourceMap implements LocationMap {
    private TreeMap<Location, Location> srcToDst = new TreeMap<>();
    private TreeMap<Location, Location> dstToSrc = new TreeMap<>();

    public Location srcToDst(Location loc) {
        if (srcToDst.containsKey(loc)) return loc;

        var tail = srcToDst.tailMap(loc);
        if (tail.isEmpty()) return null;

        var key = tail.firstKey();
        if (key != null) return srcToDst.get(key);
        else return null;
    }
    public Location dstToSrc(Location loc) {
        if (dstToSrc.containsKey(loc)) return loc;

        var tail = dstToSrc.tailMap(loc);
        if (tail.isEmpty()) return null;

        var key = tail.firstKey();
        if (key != null) return dstToSrc.get(key);
        else return null;
    }

    public static SourceMap parse(String raw) {
        var res = new SourceMap();
        var json = JSON.parse(null, raw).map();
        if (json.number("version") != 3) throw new IllegalArgumentException("Source map version must be 3.");
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

                System.out.printf("%s -> %s\n", src, dst);

                res.srcToDst.put(src, dst);
                res.dstToSrc.put(dst, src);
            }
        }

        return res;
    }
}
