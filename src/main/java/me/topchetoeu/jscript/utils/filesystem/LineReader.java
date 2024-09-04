package me.topchetoeu.jscript.utils.filesystem;

import java.io.IOException;
import java.util.Iterator;


public interface LineReader {
    String readLine() throws IOException;

    public static LineReader ofIterator(Iterator<String> it) {
        return () -> {
            if (it.hasNext()) return it.next();
            else return null;
        };
    }
}