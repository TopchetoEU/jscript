package me.topchetoeu.jscript.utils.filesystem;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

import me.topchetoeu.jscript.common.Buffer;
import me.topchetoeu.jscript.common.Filename;

public class MemoryFilesystem implements Filesystem {
    public final Mode mode;
    private HashMap<Path, Buffer> files = new HashMap<>();
    private HashSet<Path> folders = new HashSet<>();
    private HandleManager handles = new HandleManager();

    private Path realPath(String path) {
        return Filename.normalize(path);
    }

    @Override public String normalize(String... path) {
        return Paths.normalize(path);
    }
    @Override public File open(String _path, Mode perms) {
        try {
            var path = realPath(_path);
            var pcount = path.getNameCount();

            if (files.containsKey(path)) return handles.put(new MemoryFile(files.get(path), perms));
            else if (folders.contains(path)) {
                var res = new StringBuilder();

                for (var folder : folders) {
                    if (pcount + 1 != folder.getNameCount()) continue;
                    if (!folder.startsWith(path)) continue;
                    res.append(folder.toFile().getName()).append('\n');
                }

                for (var file : files.keySet()) {
                    if (pcount + 1 != file.getNameCount()) continue;
                    if (!file.startsWith(path)) continue;
                    res.append(file.toFile().getName()).append('\n');
                }

                return handles.put(new MemoryFile(new Buffer(res.toString().getBytes()), perms.intersect(Mode.READ)));
            }
            else throw new FilesystemException(ErrorReason.DOESNT_EXIST);
        }
        catch (FilesystemException e) { throw e.setPath(_path).setAction(ActionType.OPEN); }
    }
    @Override public boolean create(String _path, EntryType type) {
        try {
            var path = realPath(_path);
    
            switch (type) {
                case FILE:
                    if (!folders.contains(path.getParent())) throw new FilesystemException(ErrorReason.NO_PARENT);
                    if (folders.contains(path) || files.containsKey(path)) return false;
                    files.put(path, new Buffer());
                    return true;
                case FOLDER:
                    if (!folders.contains(path.getParent())) throw new FilesystemException(ErrorReason.NO_PARENT);
                    if (folders.contains(path) || files.containsKey(path)) return false;
                    folders.add(path);
                    return true;
                default:
                case NONE:
                    return folders.remove(path) || files.remove(path) != null;
            }
        }
        catch (FilesystemException e) { throw e.setPath(_path).setAction(ActionType.CREATE); }
    }
    @Override public FileStat stat(String _path) {
        var path = realPath(_path);

        if (files.containsKey(path)) return new FileStat(mode, EntryType.FILE);
        else if (folders.contains(path)) return new FileStat(mode, EntryType.FOLDER);
        else return new FileStat(Mode.NONE, EntryType.NONE);
    }
    @Override public void close() throws FilesystemException {
        handles.close();
    }

    public MemoryFilesystem put(String path, byte[] data) {
        var _path = realPath(path);
        var _curr = "/";

        for (var seg : _path) {
            create(_curr, EntryType.FOLDER);
            _curr += seg + "/";
        }

        files.put(_path, new Buffer(data));
        return this;
    }

    public MemoryFilesystem(Mode mode) {
        this.mode = mode;
        folders.add(Path.of("/"));
    }
}
