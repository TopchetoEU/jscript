package me.topchetoeu.jscript.utils.filesystem;

import java.util.ArrayList;

import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Values;

public class FilesystemException extends RuntimeException {
    public final ErrorReason reason;
    public final String details;
    private ActionType action;
    private EntryType entry = EntryType.FILE;
    private String path;

    public FilesystemException setPath(String path) {
        this.path = path;
        return this;
    }
    public FilesystemException setAction(ActionType action) {
        if (action == null) action = ActionType.UNKNOWN;

        this.action = action;
        return this;
    }
    public FilesystemException setEntry(EntryType entry) {
        if (entry == null) entry = EntryType.NONE;

        this.entry = entry;
        return this;
    }

    public ActionType action() {
        return action;
    }
    public String path() {
        return path;
    }
    public EntryType entry() {
        return entry;
    }

    public EngineException toEngineException() {
        var res = EngineException.ofError("IOError", getMessage());

        Values.setMember(null, res.value, "action", action.code);
        Values.setMember(null, res.value, "reason", reason.code);
        Values.setMember(null, res.value, "path", path);
        Values.setMember(null, res.value, "entry", entry.name);
        if (details != null) Values.setMember(null, res.value, "details", details);

        return res;
    }

    @Override public String getMessage() {
        var parts = new ArrayList<String>(10);

        parts.add(action == null ? "An action performed upon " : action.readable(reason.usePast));

        if (entry == EntryType.FILE) parts.add("file");
        if (entry == EntryType.FOLDER) parts.add("folder");

        if (path != null && !path.isBlank()) parts.add(path.trim());

        parts.add(reason.readable);

        var msg = String.join(" ", parts);
        if (details != null) msg += ": " + details;

        return msg;
    }

    public FilesystemException(ErrorReason type, String details) {
        super();
        if (type == null) type = ErrorReason.UNKNOWN;

        this.details = details;
        this.reason = type;
    }
    public FilesystemException(ErrorReason type) {
        this(type, null);
    }
    public FilesystemException() {
        this(null);
    }

}
