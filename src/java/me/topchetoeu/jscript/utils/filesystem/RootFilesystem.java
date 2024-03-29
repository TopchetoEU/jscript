package me.topchetoeu.jscript.utils.filesystem;

import java.util.HashMap;
import java.util.Map;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.utils.permissions.Matcher;
import me.topchetoeu.jscript.utils.permissions.Permission;
import me.topchetoeu.jscript.utils.permissions.PermissionsProvider;

public class RootFilesystem implements Filesystem {
    public final Map<String, Filesystem> protocols = new HashMap<>();
    public final PermissionsProvider perms;

    public static final Permission PERM_READ = new Permission("jscript.file.read", Matcher.fileWildcard());
    public static final Permission PERM_WRITE = new Permission("jscript.file.read", Matcher.fileWildcard());

    private boolean canRead(String _path) {
        return perms.hasPermission(PERM_READ, _path);
    }
    private boolean canWrite(String _path) {
        return perms.hasPermission(PERM_WRITE, _path);
    }

    private void modeAllowed(String _path, Mode mode) throws FilesystemException {
        if (mode.readable && perms != null && !canRead(_path)) {
            throw new FilesystemException(ErrorReason.NO_PERMISSION, "No read permissions").setPath(_path);
        }
        if (mode.writable && perms != null && !canWrite(_path)) {
            throw new FilesystemException(ErrorReason.NO_PERMISSION, "No wtrite permissions").setPath(_path);
        }
    }

    private Filesystem getProtocol(Filename filename) {
        var protocol = protocols.get(filename.protocol);

        if (protocol == null) {
            throw new FilesystemException(ErrorReason.DOESNT_EXIST, "The protocol '" + filename.protocol + "' doesn't exist.");
        }

        return protocol;
    }

    @Override public String normalize(String... paths) {
        if (paths.length == 0) return "file://";
        else {
            var filename = Filename.parse(paths[0]);
            var protocol = protocols.get(filename.protocol);
            paths[0] = filename.path;


            if (protocol == null) return Paths.normalize(paths);
            else return filename.protocol + "://" + protocol.normalize(paths);
        }
    }
    @Override public synchronized File open(String path, Mode perms) throws FilesystemException {
        try {
            var filename = Filename.parse(path);
            var protocol = getProtocol(filename);

            modeAllowed(filename.toString(), perms);
            return protocol.open(filename.path, perms);
        }
        catch (FilesystemException e) { throw e.setPath(path).setAction(ActionType.OPEN); }
    }
    @Override public synchronized boolean create(String path, EntryType type) throws FilesystemException {
        try {
            var filename = Filename.parse(path);
            var protocol = getProtocol(filename);

            modeAllowed(filename.toString(), Mode.WRITE);
            return protocol.create(filename.path, type);
        }
        catch (FilesystemException e) { throw e.setPath(path).setAction(ActionType.CREATE); }
    }
    @Override public synchronized FileStat stat(String path) throws FilesystemException {
        try {
            var filename = Filename.parse(path);
            var protocol = getProtocol(filename);

            return protocol.stat(filename.path);
        }
        catch (FilesystemException e) { throw e.setPath(path).setAction(ActionType.STAT); }
    }
    @Override public synchronized void close() throws FilesystemException {
        try {
            for (var protocol : protocols.values()) {
                protocol.close();
            }

            protocols.clear();
        }
        catch (FilesystemException e) { throw e.setAction(ActionType.CLOSE_FS); }
    }

    public RootFilesystem(PermissionsProvider perms) {
        this.perms = perms;
    }
}
