package me.topchetoeu.jscript.filesystem;

import java.io.IOException;
import java.nio.file.Path;

public interface Filesystem extends PermissionsProvider {
    public static enum EntryType {
        NONE,
        FILE,
        FOLDER,
    }

    File open(Path path) throws IOException;
    EntryType type(Path path);
    boolean mkdir(Path path);
    boolean rm(Path path) throws IOException;
}
