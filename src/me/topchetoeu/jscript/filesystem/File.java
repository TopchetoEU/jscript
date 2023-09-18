package me.topchetoeu.jscript.filesystem;

import java.io.IOException;

public interface File {
    int read() throws IOException, InterruptedException;
    boolean write(byte val) throws IOException, InterruptedException;
    long tell() throws IOException, InterruptedException;
    void seek(long offset, int pos) throws IOException, InterruptedException;
    void close() throws IOException, InterruptedException;
    Permissions perms();

    default String readToString() throws IOException, InterruptedException {
        seek(0, 2);
        long len = tell();
        if (len < 0) return null;

        seek(0, 0);
        byte[] res = new byte[(int)len];

        for (var i = 0; i < len; i++) {
            res[i] = (byte)read();
        }

        return new String(res);
    }
}