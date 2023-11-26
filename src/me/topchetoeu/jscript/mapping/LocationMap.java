package me.topchetoeu.jscript.mapping;

import me.topchetoeu.jscript.Location;

public interface LocationMap {
    Location srcToDst(Location loc);
    Location dstToSrc(Location loc);

    default LocationMap chain(LocationMap next) {
        var self = this;
        return new LocationMap() {
            public Location dstToSrc(Location loc) { return self.dstToSrc(next.dstToSrc(loc)); }
            public Location srcToDst(Location loc) { return next.srcToDst(self.srcToDst(loc)); }
        };
    }
}
