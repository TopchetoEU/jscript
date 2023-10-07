package me.topchetoeu.jscript.exceptions;

public class InterruptException extends RuntimeException {
    public InterruptException() { }
    public InterruptException(InterruptedException e) {
        super(e);
    }
}
