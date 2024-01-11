package me.topchetoeu.jscript.utils.filesystem;

import me.topchetoeu.jscript.core.engine.values.Values;
import me.topchetoeu.jscript.core.exceptions.EngineException;

public class FilesystemException extends RuntimeException {
    public static enum FSCode {
        DOESNT_EXIST(0x1),
        NOT_FILE(0x2),
        NOT_FOLDER(0x3),
        NO_PERMISSIONS_R(0x4),
        NO_PERMISSIONS_RW(0x5),
        FOLDER_NOT_EMPTY(0x6),
        ALREADY_EXISTS(0x7),
        FOLDER_EXISTS(0x8),
        UNSUPPORTED_OPERATION(0x9);

        public final int code;

        private FSCode(int code) { this.code = code; }
    }

    public static final String[] MESSAGES = {
        "How did we get here?",
        "The file or folder '%s' doesn't exist or is inaccessible.",
        "'%s' is not a file",
        "'%s' is not a folder",
        "No permissions to read '%s'",
        "No permissions to write '%s'",
        "Can't delete '%s', since it is a full folder.",
        "'%s' already exists.",
        "An unsupported operation was performed on the file '%s'."
    };

    public final String message, filename;
    public final FSCode code;

    public FilesystemException(String message, String filename, FSCode code) {
        super(code + ": " + String.format(message, filename));
        this.message = message;
        this.code = code;
        this.filename = filename;
    }
    public FilesystemException(String filename, FSCode code) {
        super(code + ": " + String.format(MESSAGES[code.code], filename));
        this.message = MESSAGES[code.code];
        this.code = code;
        this.filename = filename;
    }

    public EngineException toEngineException() {
        var res = EngineException.ofError("IOError", getMessage());
        Values.setMember(null, res.value, "code", code);
        Values.setMember(null, res.value, "filename", filename.toString());
        return res;
    }
}
