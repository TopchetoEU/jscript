package me.topchetoeu.jscript.filesystem;

public interface Filesystem {
    String normalize(String... path);
    File open(String path, Mode mode) throws FilesystemException;
    void create(String path, EntryType type) throws FilesystemException;
    FileStat stat(String path) throws FilesystemException;
}