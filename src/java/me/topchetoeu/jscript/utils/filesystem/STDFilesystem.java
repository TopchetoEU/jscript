package me.topchetoeu.jscript.utils.filesystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class STDFilesystem implements Filesystem {
    private final HashMap<String, File> handles = new HashMap<>();

    @Override
    public String normalize(String... path) {
        var res = Paths.normalize(path);
        while (res.startsWith("/")) res = res.substring(1);
        return res;
    }

    @Override public File open(String path, Mode mode) {
        path = normalize(path);
        if (handles.containsKey(path)) return handles.get(path);
        else throw new FilesystemException(ErrorReason.DOESNT_EXIST).setAction(ActionType.OPEN).setPath(path);
    }
    @Override public FileStat stat(String path) {
        path = normalize(path);
        if (handles.containsKey(path)) return new FileStat(Mode.READ_WRITE, EntryType.FILE);
        else return new FileStat(Mode.NONE, EntryType.NONE);
    }
    @Override public void close() {
        handles.clear();
    }

    public STDFilesystem add(String name, File handle) {
        this.handles.put(name, handle);
        return this;
    }

    public static STDFilesystem ofStd(InputStream in, OutputStream out, OutputStream err) {
        return new STDFilesystem()
            .add("in", File.ofStream(in))
            .add("out", File.ofStream(out))
            .add("err", File.ofStream(err));
    }
}
