package me.topchetoeu.jscript.utils.filesystem;

import me.topchetoeu.jscript.core.Extensions;
import me.topchetoeu.jscript.core.values.Symbol;

public interface Filesystem {
    public static final Symbol ENV_KEY = Symbol.get("Environment.fs");

    default String normalize(String... path) { return Paths.normalize(path); }
    default boolean create(String path, EntryType type) { throw new FilesystemException(ErrorReason.UNSUPPORTED).setAction(ActionType.CREATE); }
    File open(String path, Mode mode);
    FileStat stat(String path);
    void close();

    public static Filesystem get(Extensions exts) {
        return exts.get(ENV_KEY);
    }
}