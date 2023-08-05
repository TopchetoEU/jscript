package me.topchetoeu.jscript.filesystem;

import java.io.IOException;

public class MemoryFile implements File {
    public byte[] data;
    private int ptr = 0;

    @Override
    public void close() {
        data = null;
        ptr = -1;
    }

    @Override
    public int read(byte[] buff) throws IOException {
        if (data == null) return -1;
        if (ptr == data.length) return -1;
        int n = Math.min(buff.length, data.length - ptr);
        System.arraycopy(data, ptr, buff, 0, n);
        return n;
    }

    @Override
    public void seek(long offset, int pos) throws IOException {
        if (data == null) return;
        if (pos == 0) ptr = (int)offset;
        else if (pos == 1) ptr += (int)offset;
        else ptr = data.length - (int)offset;

        ptr = (int)Math.max(Math.min(ptr, data.length), 0);
    }

    @Override
    public long tell() throws IOException {
        if (data == null) return -1;
        return ptr;
    }

    @Override
    public void write(byte[] buff) throws IOException { }
    @Override
    public Permissions perms() {
        if (data == null) return Permissions.NONE;
        else return Permissions.READ;
    }

    public MemoryFile(byte[] data) {
        this.data = data;
    }
}
