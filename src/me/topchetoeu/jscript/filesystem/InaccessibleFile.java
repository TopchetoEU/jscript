package me.topchetoeu.jscript.filesystem;

import java.io.IOException;

public class InaccessibleFile implements File {
    public static final InaccessibleFile INSTANCE = new InaccessibleFile();

    @Override
    public int read() throws IOException, InterruptedException {
        return -1;
    }

    @Override
    public boolean write(byte val) throws IOException, InterruptedException {
        return false;
    }

    @Override
    public long tell() throws IOException, InterruptedException {
        return 0;
    }

    @Override
    public void seek(long offset, int pos) throws IOException, InterruptedException { }

    @Override
    public void close() throws IOException, InterruptedException { }

    @Override
    public Permissions perms() {
        return Permissions.NONE;
    }

    private InaccessibleFile() { }
}
