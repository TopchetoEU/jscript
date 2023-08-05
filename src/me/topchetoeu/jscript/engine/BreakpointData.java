package me.topchetoeu.jscript.engine;

import me.topchetoeu.jscript.Location;

public record BreakpointData(Location loc, CallContext ctx) { }