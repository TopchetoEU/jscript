package me.topchetoeu.jscript.utils.filesystem;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.environment.Key;

public interface Filesystem {
    public static final Key<Filesystem> KEY = new Key<>();

    default String normalize(String... path) { return Paths.normalize(path); }
    default boolean create(String path, EntryType type) { throw new FilesystemException(ErrorReason.UNSUPPORTED).setAction(ActionType.CREATE); }
    File open(String path, Mode mode);
    FileStat stat(String path);
    void close();

    public static Filesystem get(Environment exts) {
        return exts.get(KEY);
    }
}