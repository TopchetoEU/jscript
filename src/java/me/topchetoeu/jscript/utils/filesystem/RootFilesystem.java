package me.topchetoeu.jscript.utils.filesystem;

import java.util.HashMap;
import java.util.Map;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.utils.filesystem.FilesystemException.FSCode;
import me.topchetoeu.jscript.utils.permissions.Matcher;
import me.topchetoeu.jscript.utils.permissions.PermissionsProvider;

public class RootFilesystem implements Filesystem {
    public final Map<String, Filesystem> protocols = new HashMap<>();
    public final PermissionsProvider perms;

    private boolean canRead(String _path) {
        return perms.hasPermission("jscript.file.read:" + _path, Matcher.fileWildcard());
    }
    private boolean canWrite(String _path) {
        return perms.hasPermission("jscript.file.write:" + _path, Matcher.fileWildcard());
    }

    private void modeAllowed(String _path, Mode mode) throws FilesystemException {
        if (mode.readable && perms != null && !canRead(_path)) throw new FilesystemException(_path, FSCode.NO_PERMISSIONS_R);
        if (mode.writable && perms != null && !canWrite(_path)) throw new FilesystemException(_path, FSCode.NO_PERMISSIONS_RW);
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
    @Override public File open(String path, Mode perms) throws FilesystemException {
        var filename = Filename.parse(path);
        var protocol = protocols.get(filename.protocol);
        if (protocol == null) throw new FilesystemException(filename.toString(), FSCode.DOESNT_EXIST);
        modeAllowed(filename.toString(), perms);

        try { return protocol.open(filename.path, perms); }
        catch (FilesystemException e) { throw new FilesystemException(filename.toString(), e.code); }
    }
    @Override public void create(String path, EntryType type) throws FilesystemException {
        var filename = Filename.parse(path);
        var protocol = protocols.get(filename.protocol);
        if (protocol == null) throw new FilesystemException(filename.toString(), FSCode.DOESNT_EXIST);
        modeAllowed(filename.toString(), Mode.READ_WRITE);

        try { protocol.create(filename.path, type); }
        catch (FilesystemException e) { throw new FilesystemException(filename.toString(), e.code); }
    }
    @Override public FileStat stat(String path) throws FilesystemException {
        var filename = Filename.parse(path);
        var protocol = protocols.get(filename.protocol);
        if (protocol == null) throw new FilesystemException(filename.toString(), FSCode.DOESNT_EXIST);

        try { return protocol.stat(filename.path); }
        catch (FilesystemException e) { throw new FilesystemException(filename.toString(), e.code); }
    }

    public RootFilesystem(PermissionsProvider perms) {
        this.perms = perms;
    }
}
