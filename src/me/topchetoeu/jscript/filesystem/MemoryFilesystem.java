package me.topchetoeu.jscript.filesystem;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

import me.topchetoeu.jscript.Buffer;
import me.topchetoeu.jscript.filesystem.FilesystemException.FSCode;

public class MemoryFilesystem implements Filesystem {
    public final Mode mode;
    private HashMap<Path, Buffer> files = new HashMap<>();
    private HashSet<Path> folders = new HashSet<>();

    private Path getPath(String name) {
        return Path.of("/" + name.replace("\\", "/")).normalize();
    }

    @Override
    public void create(String path, EntryType type) {
        var _path = getPath(path);

        switch (type) {
            case FILE:
                if (!folders.contains(_path.getParent())) throw new FilesystemException(path, FSCode.DOESNT_EXIST);
                if (folders.contains(_path) || files.containsKey(_path)) throw new FilesystemException(path, FSCode.ALREADY_EXISTS);
                if (folders.contains(_path)) throw new FilesystemException(path, FSCode.ALREADY_EXISTS);
                files.put(_path, new Buffer());
                break;
            case FOLDER:
                if (!folders.contains(_path.getParent())) throw new FilesystemException(path, FSCode.DOESNT_EXIST);
                if (folders.contains(_path) || files.containsKey(_path)) throw new FilesystemException(path, FSCode.ALREADY_EXISTS);
                folders.add(_path);
                break;
            default:
            case NONE:
                if (!folders.remove(_path) && files.remove(_path) == null) throw new FilesystemException(path, FSCode.DOESNT_EXIST);
        }
    }

    @Override
    public File open(String path, Mode perms) {
        var _path = getPath(path);
        var pcount = _path.getNameCount();

        if (files.containsKey(_path)) return new MemoryFile(path, files.get(_path), perms);
        else if (folders.contains(_path)) {
            var res = new StringBuilder();
            for (var folder : folders) {
                if (pcount + 1 != folder.getNameCount()) continue;
                if (!folder.startsWith(_path)) continue;
                res.append(folder.toFile().getName()).append('\n');
            }
            for (var file : files.keySet()) {
                if (pcount + 1 != file.getNameCount()) continue;
                if (!file.startsWith(_path)) continue;
                res.append(file.toFile().getName()).append('\n');
            }
            return new MemoryFile(path, new Buffer(res.toString().getBytes()), perms.intersect(Mode.READ));
        }
        else throw new FilesystemException(path, FSCode.DOESNT_EXIST);
    }

    @Override
    public FileStat stat(String path) {
        var _path = getPath(path);

        if (files.containsKey(_path)) return new FileStat(mode, EntryType.FILE);
        else if (folders.contains(_path)) return new FileStat(mode, EntryType.FOLDER);
        else throw new FilesystemException(path, FSCode.DOESNT_EXIST);
    }

    public MemoryFilesystem put(String path, byte[] data) {
        var _path = getPath(path);
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
