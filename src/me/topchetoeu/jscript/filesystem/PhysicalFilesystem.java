package me.topchetoeu.jscript.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import me.topchetoeu.jscript.filesystem.FilesystemException.FSCode;

public class PhysicalFilesystem implements Filesystem {
    public final Path root;

    private Path getPath(String name) {
        return root.resolve(name.replace("\\", "/")).normalize();
    }

    private void checkMode(Path path, Mode mode) {
        if (!path.startsWith(root)) throw new FilesystemException(path.toString(), FSCode.NO_PERMISSIONS_R);
        if (mode.readable && !path.toFile().canRead()) throw new FilesystemException(path.toString(), FSCode.NO_PERMISSIONS_R);
        if (mode.writable && !path.toFile().canWrite()) throw new FilesystemException(path.toString(), FSCode.NO_PERMISSIONS_RW);
    }

    @Override
    public File open(String path, Mode perms) {
        var _path = getPath(path);
        var f = _path.toFile();

        checkMode(_path, perms);


        if (f.isDirectory()) return MemoryFile.fromFileList(path, f.listFiles());
        else try { return new PhysicalFile(path, perms); }
        catch (FileNotFoundException e) { throw new FilesystemException(_path.toString(), FSCode.DOESNT_EXIST); }
    }

    @Override
    public void create(String path, EntryType type) {
        var _path = getPath(path);
        var f = _path.toFile();

        checkMode(_path, Mode.READ_WRITE);
        switch (type) {
            case FILE:
                try {
                    if (!f.createNewFile()) throw new FilesystemException(_path.toString(), FSCode.ALREADY_EXISTS);
                    else break;
                } 
                catch (IOException e) { throw new FilesystemException(_path.toString(), FSCode.NO_PERMISSIONS_RW); }
            case FOLDER:
                if (!f.mkdir()) throw new FilesystemException(_path.toString(), FSCode.ALREADY_EXISTS);
                else break;
            case NONE:
            default:
                if (!f.delete()) throw new FilesystemException(_path.toString(), FSCode.DOESNT_EXIST);
                else break;
        }
    }

    @Override
    public FileStat stat(String path) {
        var _path = getPath(path);
        var f = _path.toFile();

        if (f.exists()) throw new FilesystemException(_path.toString(), FSCode.DOESNT_EXIST);
        checkMode(_path, Mode.READ);

        return new FileStat(
            f.canWrite() ? Mode.READ_WRITE : Mode.READ,
            f.isFile() ? EntryType.FILE : EntryType.FOLDER
        );
    }

    public PhysicalFilesystem(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }
}
