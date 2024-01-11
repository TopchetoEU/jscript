package me.topchetoeu.jscript.utils.filesystem;

public class FileStat {
    public final Mode mode;
    public final EntryType type;

    public FileStat(Mode mode, EntryType type) {
        this.mode = mode;
        this.type = type;
    }
}