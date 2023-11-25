package me.topchetoeu.jscript.filesystem;

public enum Mode {
    NONE("", false, false),
    READ("r", true, false),
    READ_WRITE("rw", true, true);

    public final String name;
    public final boolean readable;
    public final boolean writable;

    public Mode intersect(Mode other) {
        if (this == NONE || other == NONE) return NONE;
        if (this == READ_WRITE && other == READ_WRITE) return READ_WRITE;
        return READ;
    }

    private Mode(String mode, boolean r, boolean w) {
        this.name = mode;
        this.readable = r;
        this.writable = w;
    }

    public static Mode parse(String mode) {
        switch (mode) {
            case "r": return READ;
            case "rw": return READ_WRITE;
            default: return NONE;
        }
    }
}
