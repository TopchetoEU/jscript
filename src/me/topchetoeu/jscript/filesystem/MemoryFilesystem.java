package me.topchetoeu.jscript.filesystem;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

import me.topchetoeu.jscript.Buffer;
import me.topchetoeu.jscript.Filename;
import me.topchetoeu.jscript.filesystem.FilesystemException.FSCode;

public class MemoryFilesystem implements Filesystem {
    public final Mode mode;
    private HashMap<Path, Buffer> files = new HashMap<>();
    private HashSet<Path> folders = new HashSet<>();

    private Path realPath(String path) {
        return Filename.normalize(path);
    }

    @Override
    public String normalize(String... path) {
        return Paths.normalize(path);
    }

    @Override
    public void create(String _path, EntryType type) {
        var path = realPath(_path);

        switch (type) {
            case FILE:
                if (!folders.contains(path.getParent())) throw new FilesystemException(path.toString(), FSCode.DOESNT_EXIST);
                if (folders.contains(path) || files.containsKey(path)) throw new FilesystemException(path.toString(), FSCode.ALREADY_EXISTS);
                if (folders.contains(path)) throw new FilesystemException(path.toString(), FSCode.ALREADY_EXISTS);
                files.put(path, new Buffer());
                break;
            case FOLDER:
                if (!folders.contains(path.getParent())) throw new FilesystemException(_path, FSCode.DOESNT_EXIST);
                if (folders.contains(path) || files.containsKey(path)) throw new FilesystemException(path.toString(), FSCode.ALREADY_EXISTS);
                folders.add(path);
                break;
            default:
            case NONE:
                if (!folders.remove(path) && files.remove(path) == null) throw new FilesystemException(path.toString(), FSCode.DOESNT_EXIST);
        }
    }

    @Override
    public File open(String _path, Mode perms) {
        var path = realPath(_path);
        var pcount = path.getNameCount();

        if (files.containsKey(path)) return new MemoryFile(path.toString(), files.get(path), perms);
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
            return new MemoryFile(path.toString(), new Buffer(res.toString().getBytes()), perms.intersect(Mode.READ));
        }
        else throw new FilesystemException(path.toString(), FSCode.DOESNT_EXIST);
    }

    @Override
    public FileStat stat(String _path) {
        var path = realPath(_path);

        if (files.containsKey(path)) return new FileStat(mode, EntryType.FILE);
        else if (folders.contains(path)) return new FileStat(mode, EntryType.FOLDER);
        else return new FileStat(Mode.NONE, EntryType.NONE);
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
