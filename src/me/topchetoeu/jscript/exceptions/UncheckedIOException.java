package me.topchetoeu.jscript.exceptions;

import java.io.IOException;

public class UncheckedIOException extends RuntimeException {
    public UncheckedIOException(IOException e) {
        super(e);
    }
}
