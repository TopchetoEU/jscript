package me.topchetoeu.jscript.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class PhysicalFilesystem implements Filesystem {
    public final Path root;
    public final PermissionsProvider perms;

    private static Path joinPaths(Path root, Path file) {
        if (file.isAbsolute()) file = file.getRoot().relativize(file);
        file = file.normalize();

        while (true) {
            if (file.startsWith("..")) file = file.subpath(1, file.getNameCount());
            else if (file.startsWith(".")) file = file.subpath(1, file.getNameCount());
            else break;
        }

        return Path.of(root.toString(), file.toString());
    }

    @Override
    public boolean mkdir(Path path) {
        if (!perms(path).writable) return false;
        path = joinPaths(root, path);
        return path.toFile().mkdirs();
    }
    @Override
    public File open(Path path) throws IOException {
        var perms = perms(path);
        if (perms == Permissions.NONE) return new InaccessibleFile();
        path = joinPaths(root, path);

        if (path.toFile().isDirectory()) {
            return new MemoryFile(String.join("\n", Files.list(path).map(Path::toString).collect(Collectors.toList())).getBytes());
        }
        else if (path.toFile().isFile()) {
            return new PhysicalFile(path, perms);
        }
        else return new InaccessibleFile();
    }
    @Override
    public boolean rm(Path path) {
        if (!perms(path).writable) return false;
        return joinPaths(root, path).toFile().delete();
    }
    @Override
    public EntryType type(Path path) {
        if (!perms(path).readable) return EntryType.NONE;
        path = joinPaths(root, path);

        if (!path.toFile().exists()) return EntryType.NONE;
        if (path.toFile().isFile()) return EntryType.FILE;
        else return EntryType.FOLDER;

    }
    @Override
    public Permissions perms(Path path) {
        path = joinPaths(root, path);
        var res = perms.perms(path);

        if (!path.toFile().canWrite() && res.writable) res = Permissions.READ;
        if (!path.toFile().canRead() && res.readable) res = Permissions.NONE;

        return res;
    }

    public PhysicalFilesystem(Path root, PermissionsProvider perms) {
        this.root = root;
        this.perms = perms;
    }
}
