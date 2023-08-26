package me.topchetoeu.jscript.engine;

import me.topchetoeu.jscript.Location;

public class BreakpointData {
    public final Location loc;
    public final CallContext ctx;

    public BreakpointData(Location loc, CallContext ctx) {
        this.loc = loc;
        this.ctx = ctx;
    }
}