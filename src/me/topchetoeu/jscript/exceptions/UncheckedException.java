package me.topchetoeu.jscript.exceptions;

public class UncheckedException extends RuntimeException {
    public UncheckedException(Throwable err) {
        super(err);
    }
}
