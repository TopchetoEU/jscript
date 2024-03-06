package me.topchetoeu.jscript.runtime.exceptions;

public class InterruptException extends RuntimeException {
    public InterruptException() { }
    public InterruptException(Throwable e) {
        super(e);
    }
}
