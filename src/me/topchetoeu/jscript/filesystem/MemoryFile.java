package me.topchetoeu.jscript.filesystem;

import java.io.IOException;

public class MemoryFile implements File {
    private int ptr;
    private Permissions mode;
    public final byte[] data;

    @Override
    public int read() throws IOException, InterruptedException {
        if (data == null || !mode.readable || ptr >= data.length) return -1;
        return data[ptr++];
    }

    @Override
    public boolean write(byte val) throws IOException, InterruptedException {
        if (data == null || !mode.writable || ptr >= data.length) return false;
        data[ptr++] = val;
        return true;
    }

    @Override
    public long tell() throws IOException, InterruptedException {
        return ptr;
    }

    @Override
    public void seek(long offset, int pos) throws IOException, InterruptedException {
        if (data == null) return;

        if (pos == 0) ptr = (int)offset;
        else if (pos == 1) ptr += (int)offset;
        else if (pos == 2) ptr = data.length - (int)offset;
    }

    @Override
    public void close() throws IOException, InterruptedException {
        mode = null;
        ptr = 0;
    }

    @Override
    public Permissions perms() {
        if (data == null) return Permissions.NONE;
        return mode;
    }

    public MemoryFile(byte[] buff, Permissions mode) {
        this.data = buff;
        this.mode = mode;
    }
}
