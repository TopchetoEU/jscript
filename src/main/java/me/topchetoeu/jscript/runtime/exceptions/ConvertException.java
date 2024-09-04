package me.topchetoeu.jscript.runtime.exceptions;

public class ConvertException extends RuntimeException {
    public final String source, target;

    public ConvertException(String source, String target) {
        super(String.format("Cannot convert '%s' to '%s'.", source, target));
        this.source = source;
        this.target = target;
    }
}
