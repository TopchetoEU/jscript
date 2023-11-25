package me.topchetoeu.jscript.filesystem;

import java.util.HashMap;
import java.util.Map;

import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.filesystem.FilesystemException.FSCode;
import me.topchetoeu.jscript.permissions.PermissionsManager;

public class RootFilesystem implements Filesystem {
    public final Map<String, Filesystem> protocols = new HashMap<>();
    public final PermissionsManager perms;

    private boolean canRead(PermissionsManager perms, String _path) {
        return perms.has("jscript.file.read:" + _path, '/');
    }
    private boolean canWrite(PermissionsManager perms, String _path) {
        return perms.has("jscript.file.write:" + _path, '/');
    }

    private void modeAllowed(String _path, Mode mode) throws FilesystemException {
        if (mode.readable && perms != null && !canRead(perms, _path)) throw new FilesystemException(_path, FSCode.NO_PERMISSIONS_R);
        if (mode.writable && perms != null && !canWrite(perms, _path)) throw new FilesystemException(_path, FSCode.NO_PERMISSIONS_RW);
    }

    @Override
    public File open(String path, Mode perms) throws FilesystemException {
        var filename = Filename.parse(path);
        var protocol = protocols.get(filename.protocol);
        if (protocol == null) throw new FilesystemException(filename.toString(), FSCode.DOESNT_EXIST);
        modeAllowed(filename.toString(), perms);

        try { return protocol.open(filename.path, perms); }
        catch (FilesystemException e) { throw new FilesystemException(filename.toString(), e.code); }
    }
    @Override
    public void create(String path, EntryType type) throws FilesystemException {
        var filename = Filename.parse(path);
        var protocol = protocols.get(filename.protocol);
        if (protocol == null) throw new FilesystemException(filename.toString(), FSCode.DOESNT_EXIST);
        modeAllowed(filename.toString(), Mode.READ_WRITE);

        try { protocol.create(filename.path, type); }
        catch (FilesystemException e) { throw new FilesystemException(filename.toString(), e.code); }
    }
    @Override
    public FileStat stat(String path) throws FilesystemException {
        var filename = Filename.parse(path);
        var protocol = protocols.get(filename.protocol);
        if (protocol == null) throw new FilesystemException(filename.toString(), FSCode.DOESNT_EXIST);
        modeAllowed(filename.toString(), Mode.READ);

        try { return protocol.stat(path); }
        catch (FilesystemException e) { throw new FilesystemException(filename.toString(), e.code); }
    }

    public RootFilesystem(PermissionsManager perms) {
        this.perms = perms;
    }
}
