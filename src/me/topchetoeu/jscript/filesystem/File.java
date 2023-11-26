package me.topchetoeu.jscript.filesystem;

public interface File {
    int read(byte[] buff);
    void write(byte[] buff);
    long getPtr();
    void setPtr(long offset, int pos);
    void close();
    Mode mode();

    default String readToString() {
        setPtr(0, 2);
        long len = getPtr();
        if (len < 0) return null;

        setPtr(0, 0);

        byte[] res = new byte[(int)len];
        read(res);

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