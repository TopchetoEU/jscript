package me.topchetoeu.jscript.utils.filesystem;

public enum ErrorReason {
    UNKNOWN(0, "failed", false),
    NO_PERMISSION(1, "is not allowed", false),
    CLOSED(1, "that was closed", true),
    UNSUPPORTED(2, "is not supported", false),
    ILLEGAL_ARGS(3, "with illegal arguments", true),
    DOESNT_EXIST(4, "that doesn't exist", true),
    ALREADY_EXISTS(5, "that already exists", true),
    ILLEGAL_PATH(6, "with illegal path", true),
    NO_PARENT(7, "with a missing parent folder", true);

    public final int code;
    public final boolean usePast;
    public final String readable;

    private ErrorReason(int code, String readable, boolean usePast) {
        this.code = code;
        this.readable = readable;
        this.usePast = usePast;
    }
}
