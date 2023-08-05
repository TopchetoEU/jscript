package me.topchetoeu.jscript.filesystem;

import java.io.IOException;

public interface File {
    int read(byte[] buff) throws IOException;
    void write(byte[] buff) throws IOException;
    long tell() throws IOException;
    void seek(long offset, int pos) throws IOException;
    void close() throws IOException;
    Permissions perms();

    default String readToString() throws IOException {
        seek(0, 2);
        long len = tell();
        if (len < 0) return null;
        seek(0, 0);
        byte[] res = new byte[(int)len];
        if (read(res) < 0) return null;
        return new String(res);
    }
}