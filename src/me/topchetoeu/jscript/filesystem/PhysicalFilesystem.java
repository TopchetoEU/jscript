package me.topchetoeu.jscript.filesystem;

import java.io.IOException;
import java.nio.file.Path;

public class PhysicalFilesystem implements Filesystem {
    public final Path root;

    private Permissions getPerms(Path path) {
        var file = path.toFile();
        if (!path.startsWith(root)) return Permissions.NONE;
        if (file.canRead() && file.canWrite()) return Permissions.READ_WRITE;
        if (file.canRead()) return Permissions.READ;

        return Permissions.NONE;
    }
    private Path getPath(String name) {
        return root.resolve(name);
    }

    @Override
    public File open(String path) throws IOException, InterruptedException {
        var _path = root.resolve(path);

        var perms = getPerms(_path);
        if (perms == Permissions.NONE) return InaccessibleFile.INSTANCE;

        var f = _path.toFile();

        if (f.isDirectory()) {
            var res = new StringBuilder();

            for (var child : f.listFiles()) res.append(child.toString()).append('\n');

            return new MemoryFile(res.toString().getBytes(), Permissions.READ);
        }
        else return new PhysicalFile(path, perms);
    }

    @Override
    public boolean mkdir(String path) throws IOException, InterruptedException {
        var _path = getPath(path);
        var perms = getPerms(_path);
        var f = _path.toFile();

        if (!perms.writable) return false;
        else return f.mkdir();
    }

    @Override
    public EntryType type(String path) throws IOException, InterruptedException {
        var _path = getPath(path);
        var perms = getPerms(_path);
        var f = _path.toFile();

        if (perms == Permissions.NONE) return EntryType.NONE;
        else if (f.isFile()) return EntryType.FILE;
        else return EntryType.FOLDER;
    }

    @Override
    public boolean rm(String path) throws IOException, InterruptedException {
        var _path = getPath(path);
        var perms = getPerms(_path);
        var f = _path.toFile();

        if (!perms.writable) return false;
        else return f.delete();
    }

    public PhysicalFilesystem(Path root) {
        this.root = root;
    }
}
