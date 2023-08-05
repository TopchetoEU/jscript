package me.topchetoeu.jscript.filesystem;

import java.io.IOException;

public class InaccessibleFile implements File {
    @Override
    public int read(byte[] buff) throws IOException {
        return -1;
    }
    @Override
    public void write(byte[] buff) throws IOException {
    }
    @Override
    public long tell() throws IOException {
        return -1;
    }
    @Override
    public void seek(long offset, int pos) throws IOException {
    }
    @Override
    public void close() throws IOException {
    }
    @Override
    public Permissions perms() {
        return Permissions.NONE;
    }
}
