package me.topchetoeu.jscript.utils.filesystem;

public enum ActionType {
    UNKNOWN(0, "An operation performed upon", "An operation was performed upon"),
    READ(1, "Reading from", "Read from"),
    WRITE(2, "Writting to", "Wrote to"),
    SEEK(3, "Seeking in", "Sought in"),
    CLOSE(4, "Closing", "Closed"),
    STAT(5, "Stat of", "Statted"),
    OPEN(6, "Opening", "Opened"),
    CREATE(7, "Creating", "Created"),
    DELETE(8, "Deleting", "Deleted"),
    CLOSE_FS(9, "Closing filesystem", "Closed filesystem");

    public final int code;
    public final String continuous, past;

    public String readable(boolean usePast) {
        if (usePast) return past;
        else return continuous;
    }

    private ActionType(int code, String continuous, String past) {
        this.code = code;
        this.continuous = continuous;
        this.past = past;
    }
}
