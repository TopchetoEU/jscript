package me.topchetoeu.jscript.filesystem;

public enum Permissions {
    NONE("", false, false),
    READ("r", true, false),
    READ_WRITE("rw", true, true);

    public final String readMode;
    public final boolean readable;
    public final boolean writable;

    private Permissions(String mode, boolean r, boolean w) {
        this.readMode = mode;
        this.readable = r;
        this.writable = w;
    }
}
