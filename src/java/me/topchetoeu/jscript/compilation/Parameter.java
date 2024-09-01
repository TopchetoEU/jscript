package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.parsing.Location;

public final class Parameter {
    public final Location loc;
    public final String name;
    public final Node node;

    public Parameter(Location loc, String name, Node node) {
        this.name = name;
        this.node = node;
        this.loc = loc;
    }
}