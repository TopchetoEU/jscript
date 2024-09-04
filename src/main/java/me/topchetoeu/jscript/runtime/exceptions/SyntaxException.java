package me.topchetoeu.jscript.runtime.exceptions;

import me.topchetoeu.jscript.common.Location;

public class SyntaxException extends RuntimeException {
    public final Location loc;
    public final String msg;

    public SyntaxException(Location loc, String msg) {
        super(String.format("Syntax error (at %s): %s", loc, msg));
        this.loc = loc;
        this.msg = msg;
    }
}