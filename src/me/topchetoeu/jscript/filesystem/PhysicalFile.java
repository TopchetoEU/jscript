package me.topchetoeu.jscript.filesystem;

import java.io.IOException;
import java.io.RandomAccessFile;

public class PhysicalFile implements File {
    private RandomAccessFile file;
    private Permissions perms;

    @Override
    public int read() throws IOException, InterruptedException {
        if (file == null || !perms.readable) return -1;
        else return file.read();
    }

    @Override
    public boolean write(byte val) throws IOException, InterruptedException {
        if (file == null || !perms.writable) return false;
        file.write(val);
        return true;
    }

    @Override
    public long tell() throws IOException, InterruptedException {
        if (file == null) return 0;
        return file.getFilePointer();
    }
    @Override
    public void seek(long offset, int pos) throws IOException, InterruptedException {
        if (file == null) return;
        if (pos == 0) file.seek(pos);
        else if (pos == 1) file.seek(file.getFilePointer() + pos);
        else if (pos == 2) file.seek(file.length() + pos);
    }

    @Override
    public void close() throws IOException, InterruptedException {
        if (file == null) return;
        file.close();
    }

    @Override
    public Permissions perms() { return perms; }
    
    public PhysicalFile(String path, Permissions mode) throws IOException {
        if (mode == Permissions.NONE) file = null;
        else file = new RandomAccessFile(path, mode.readMode);

        perms = mode;
    }
}
