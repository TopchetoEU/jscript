package me.topchetoeu.jscript.utils.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import me.topchetoeu.jscript.utils.filesystem.FilesystemException.FSCode;

public class PhysicalFilesystem implements Filesystem {
    public final String root;

    private void checkMode(Path path, Mode mode) {
        if (!path.startsWith(root)) throw new FilesystemException(path.toString(), FSCode.NO_PERMISSIONS_R);

        if (mode.readable && !Files.isReadable(path)) throw new FilesystemException(path.toString(), FSCode.NO_PERMISSIONS_R);
        if (mode.writable && !Files.isWritable(path)) throw new FilesystemException(path.toString(), FSCode.NO_PERMISSIONS_RW);
    }

    private Path realPath(String path) {
        return Path.of(Paths.chroot(root, path));
    }

    @Override
    public String normalize(String... paths) {
        return Paths.normalize(paths);
    }

    @Override
    public File open(String _path, Mode perms) {
        _path = normalize(_path);
        var path = realPath(_path);

        checkMode(path, perms);

        try {
            if (Files.isDirectory(path)) return new ListFile(_path, Files.list(path).map((v -> v.getFileName().toString())));
            else return new PhysicalFile(_path, path.toString(), perms);
        }
        catch (IOException e) { throw new FilesystemException(path.toString(), FSCode.DOESNT_EXIST); }
    }

    @Override
    public void create(String _path, EntryType type) {
        var path = realPath(_path);

        if (type == EntryType.NONE != Files.exists(path)) throw new FilesystemException(path.toString(), FSCode.ALREADY_EXISTS);

        try {
            switch (type) {
                case FILE:
                    Files.createFile(path);
                    break;
                case FOLDER:
                    Files.createDirectories(path);
                    break;
                case NONE:
                default:
                    Files.delete(path);
            } 
        }
        catch (IOException e) { throw new FilesystemException(path.toString(), FSCode.NO_PERMISSIONS_RW); }
    }

    @Override
    public FileStat stat(String _path) {
        var path = realPath(_path);

        if (!Files.exists(path)) return new FileStat(Mode.NONE, EntryType.NONE);

        var perms = Mode.NONE;

        if (Files.isReadable(path)) {
            if (Files.isWritable(path)) perms = Mode.READ_WRITE;
            else perms = Mode.READ;
        }

        if (perms == Mode.NONE) return new FileStat(Mode.NONE, EntryType.NONE);

        return new FileStat(
            perms,
            Files.isDirectory(path) ? EntryType.FOLDER : EntryType.FILE
        );
    }

    public PhysicalFilesystem(String root) {
        this.root = Paths.normalize(Path.of(root).toAbsolutePath().toString());
    }
}
