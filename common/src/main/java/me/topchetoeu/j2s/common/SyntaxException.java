package me.topchetoeu.j2s.common;

public class SyntaxException extends RuntimeException {
	public final Location loc;
	public final String msg;

	public SyntaxException(Location loc, String msg) {
		super(String.format("Syntax error %s: %s", loc, msg));
		this.loc = loc;
		this.msg = msg;
	}
}