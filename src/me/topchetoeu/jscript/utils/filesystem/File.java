package me.topchetoeu.jscript.utils.filesystem;

import me.topchetoeu.jscript.common.Buffer;

public interface File {
    int read(byte[] buff);
    void write(byte[] buff);
    long seek(long offset, int pos);
    void close();

    default String readToString() {
        long len = seek(0, 2);
        if (len < 0) return null;
        seek(0, 0);

        byte[] res = new byte[(int)len];
        len = read(res);

        return new String(res);
    }
    default String readLine() {
        var res = new Buffer();
        var buff = new byte[1];

        while (true) {
            if (read(buff) == 0) {
                if (res.length() == 0) return null;
                else break;
            }

            if (buff[0] == '\n') break;

            res.write(res.length(), buff);
        }
        return new String(res.data());
    }
}