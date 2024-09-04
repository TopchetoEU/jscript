package me.topchetoeu.jscript.utils.filesystem;

import java.io.IOException;

public interface LineWriter {
    void writeLine(String value) throws IOException;
}