package me.topchetoeu.jscript.utils.filesystem;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class PhysicalFilesystem implements Filesystem {
    public final String root;
    private HandleManager handles = new HandleManager();

    private void checkMode(Path path, Mode mode) {
        if (!path.startsWith(root)) throw new FilesystemException(ErrorReason.NO_PERMISSION, "Tried to jailbreak the sandbox.");

        if (mode.readable && !Files.isReadable(path)) throw new FilesystemException(ErrorReason.NO_PERMISSION, "No read permissions");
        if (mode.writable && !Files.isWritable(path)) throw new FilesystemException(ErrorReason.NO_PERMISSION, "No write permissions");
    }

    private Path realPath(String path) {
        return Path.of(Paths.chroot(root, path));
    }

    @Override public String normalize(String... paths) {
        return Paths.normalize(paths);
    }
    @Override public File open(String _path, Mode perms) {
        try {
            var path = realPath(normalize(_path));
            checkMode(path, perms);
    
            try {
                if (Files.isDirectory(path)) return handles.put(File.ofIterator(
                    Files.list(path).map(v -> v.getFileName().toString()).iterator()
                ));
                else return handles.put(new PhysicalFile(path, perms));
            }
            catch (IOException e) { throw new FilesystemException(ErrorReason.DOESNT_EXIST); }
        }
        catch (FilesystemException e) { throw e.setAction(ActionType.OPEN).setPath(_path); }
    }
    @Override public boolean create(String _path, EntryType type) {
        try {
            var path = realPath(_path);

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
            catch (FileAlreadyExistsException | NoSuchFileException e) { return false; }
            catch (IOException e) { throw new FilesystemException(ErrorReason.NO_PARENT); }
        }
        catch (FilesystemException e) { throw e.setAction(ActionType.CREATE).setPath(_path); }

        return true;
    }
    @Override public FileStat stat(String _path) {
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
    @Override public void close() throws FilesystemException {
        try {
            handles.close();
        }
        catch (FilesystemException e) { throw e.setAction(ActionType.CLOSE_FS); }
    }

    public PhysicalFilesystem(String root) {
        this.root = Paths.normalize(Path.of(root).toAbsolutePath().toString());
    }
}
