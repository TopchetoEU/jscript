package me.topchetoeu.jscript.exceptions;

import me.topchetoeu.jscript.Location;

public class SyntaxException extends RuntimeException {
    public final Location loc;
    public final String msg;

    public SyntaxException(Location loc, String msg) {
        super("Syntax error (at %s): %s".formatted(loc, msg));
        this.loc = loc;
        this.msg = msg;
    }
}