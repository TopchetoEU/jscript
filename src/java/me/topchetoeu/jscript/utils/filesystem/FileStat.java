package me.topchetoeu.jscript.utils.filesystem;

public class FileStat {
    public final Mode mode;
    public final EntryType type;

    public FileStat(Mode mode, EntryType type) {
        if (mode == Mode.NONE) type = EntryType.NONE;
        if (type == EntryType.NONE) mode = Mode.NONE;

        this.mode = mode;
        this.type = type;
    }
}