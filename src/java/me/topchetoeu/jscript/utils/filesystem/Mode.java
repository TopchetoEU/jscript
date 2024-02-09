package me.topchetoeu.jscript.utils.filesystem;

public enum Mode {
    NONE("", false, false),
    READ("r", true, false),
    WRITE("rw", false, true),
    READ_WRITE("rw", true, true);

    public final String name;
    public final boolean readable;
    public final boolean writable;

    public Mode intersect(Mode other) {
        return of(readable && other.readable, writable && other.writable);
    }

    private Mode(String mode, boolean r, boolean w) {
        this.name = mode;
        this.readable = r;
        this.writable = w;
    }

    public static Mode of(boolean read, boolean write) {
        if (read && write) return READ_WRITE;
        if (read) return READ;
        if (write) return WRITE;
        return NONE;
    }

    public static Mode parse(String mode) {
        switch (mode.toLowerCase()) {
            case "r": return READ;
            case "w": return WRITE;
            case "r+":
            case "w+":
            case "wr":
            case "rw": return READ_WRITE;
            default: return NONE;
        }
    }
}
