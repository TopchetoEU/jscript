package me.topchetoeu.jscript.utils.filesystem;

import java.io.InputStream;
import java.io.OutputStream;

public class STDFilesystem implements Filesystem {
    private File in;
    private File out;
    private File err;

    @Override
    public String normalize(String... path) {
        var res = Paths.normalize(path);
        while (res.startsWith("/")) res = res.substring(1);
        while (res.endsWith("/")) res = res.substring(0, res.length() - 1);
        return res;
    }

    @Override public synchronized File open(String path, Mode mode) {
        path = normalize(path);
        if (in != null && path.equals("in")) return in;
        else if (out != null && path.equals("out")) return out;
        else if (err != null && path.equals("err")) return err;
        else throw new FilesystemException(ErrorReason.DOESNT_EXIST).setAction(ActionType.OPEN).setPath(path);
    }
    @Override public synchronized FileStat stat(String path) {
        path = normalize(path);
        if (path.equals("in") || path.equals("out") || path.equals("err")) return new FileStat(Mode.READ_WRITE, EntryType.FILE);
        else return new FileStat(Mode.NONE, EntryType.NONE);
    }
    @Override public synchronized void close() {
        in = out = err = null;
    }

    public STDFilesystem(File in, File out, File err) {
        this.in = in;
        this.out = out;
        this.err = err;
    }
    public STDFilesystem(InputStream in, OutputStream out, OutputStream err) {
        if (in != null) this.in = File.ofStream(in);
        if (out != null) this.out = File.ofStream(out);
        if (err != null) this.err = File.ofStream(err);
    }
    public STDFilesystem(LineReader in, LineWriter out) {
        if (in != null) this.in = File.ofLineReader(in);
        if (out != null) {
            this.out = File.ofLineWriter(out);
            this.err = File.ofLineWriter(out);
        }
    }
}
