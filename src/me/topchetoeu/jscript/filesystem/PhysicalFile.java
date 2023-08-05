package me.topchetoeu.jscript.filesystem;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

public class PhysicalFile implements File {
    private RandomAccessFile file;
    private Permissions perms;

    @Override
    public int read(byte[] buff) throws IOException {
        if (!perms.readable) return -1;
        return file.read(buff);
    }
    @Override
    public void write(byte[] buff) throws IOException {
        if (!perms.writable) return;
        file.write(buff);
    }
    @Override
    public long tell() throws IOException {
        if (!perms.readable) return -1;
        return file.getFilePointer();
    }
    @Override
    public void seek(long offset, int pos) throws IOException {
        if (!perms.readable) return;
        if (pos == 0) file.seek(offset);
        else if (pos == 1) file.seek(tell() + offset);
        else file.seek(file.length() + offset);
    }
    @Override
    public void close() throws IOException {
        if (!perms.readable) return;
        file.close();
        file = null;
        perms = Permissions.NONE;
    }
    @Override
    public Permissions perms() {
        return perms;
    }

    public PhysicalFile(Path path, Permissions perms) throws IOException {
        if (!path.toFile().canWrite() && perms.writable) perms = Permissions.READ;
        if (!path.toFile().canRead() && perms.readable) perms = Permissions.NONE;

        this.perms = perms;
        if (perms == Permissions.NONE) this.file = null;
        else this.file = new RandomAccessFile(path.toString(), perms.readMode);
    }
}
