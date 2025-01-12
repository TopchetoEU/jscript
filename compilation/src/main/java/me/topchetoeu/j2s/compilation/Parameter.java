package me.topchetoeu.j2s.compilation;

import me.topchetoeu.j2s.common.Location;

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