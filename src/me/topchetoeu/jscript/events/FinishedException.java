package me.topchetoeu.jscript.events;

public class FinishedException extends RuntimeException {
    public FinishedException() {
        super("The observable has ended.");
    }
}
