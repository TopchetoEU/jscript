package me.topchetoeu.jscript.utils.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.topchetoeu.jscript.common.Buffer;
import me.topchetoeu.jscript.utils.LineWriter;
import me.topchetoeu.jscript.utils.filesystem.FilesystemException.FSCode;
import me.topchetoeu.jscript.utils.permissions.Permission;
import me.topchetoeu.jscript.utils.permissions.PermissionsProvider;

public interface File {
    int read(byte[] buff);
    void write(byte[] buff);
    long seek(long offset, int pos);
    void close();

    default File wrap(String name, PermissionsProvider perms, Permission read, Permission write, Permission seek, Permission close) {
        var self = this;

        return new File() {
            @Override public int read(byte[] buff) {
                if (read != null && perms.hasPermission(read, name)) return self.read(buff);
                else throw new FilesystemException(name, FSCode.NO_PERMISSIONS_R);
            }
            @Override public void write(byte[] buff) {
                if (write != null && perms.hasPermission(write, name)) self.write(buff);
                else throw new FilesystemException(name, FSCode.NO_PERMISSIONS_RW);
            }
            @Override public long seek(long offset, int pos) {
                if (seek != null && perms.hasPermission(seek, name)) return self.seek(offset, pos);
                else throw new FilesystemException(name, FSCode.NO_PERMISSIONS_R);
            }
            @Override public void close() {
                if (close != null && perms.hasPermission(close, name)) self.close();
                else throw new FilesystemException(name, FSCode.NO_PERMISSIONS_R);
            }
        };
    }

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

    public static File ofStream(String name, InputStream str) {
        return new File() {
            @Override public int read(byte[] buff) {
                try {
                    return str.read(buff);
                }
                catch (IOException e) {
                    throw new FilesystemException(name, FSCode.NO_PERMISSIONS_R);
                }
            }
            @Override public void write(byte[] buff) {
                throw new FilesystemException(name, FSCode.NO_PERMISSIONS_RW);
            }
            @Override public long seek(long offset, int pos) {
                throw new FilesystemException(name, FSCode.UNSUPPORTED_OPERATION);
            }
            @Override public void close() {
                throw new FilesystemException(name, FSCode.UNSUPPORTED_OPERATION);
            }
        };
    }
    public static File ofStream(String name, OutputStream str) {
        return new File() {
            @Override public int read(byte[] buff) {
                throw new FilesystemException(name, FSCode.NO_PERMISSIONS_R);
            }
            @Override public void write(byte[] buff) {
                try {
                    str.write(buff);
                }
                catch (IOException e) {
                    throw new FilesystemException(name, FSCode.NO_PERMISSIONS_RW);
                }
            }
            @Override public long seek(long offset, int pos) {
                throw new FilesystemException(name, FSCode.UNSUPPORTED_OPERATION);
            }
            @Override public void close() {
                throw new FilesystemException(name, FSCode.UNSUPPORTED_OPERATION);
            }
        };
    }
    public static File ofLineWriter(String name, LineWriter writer) {
        var buff = new Buffer();

        return new File() {
            @Override public int read(byte[] buff) {
                throw new FilesystemException(name, FSCode.NO_PERMISSIONS_R);
            }
            @Override public void write(byte[] val) {
                for (var b : val) {
                    if (b == '\n') {
                        try {
                            writer.writeLine(new String(buff.data()));
                        }
                        catch (IOException e) {
                            throw new FilesystemException(name, FSCode.NO_PERMISSIONS_RW);
                        }
                    }
                    else buff.append(b);
                }
            }
            @Override public long seek(long offset, int pos) {
                throw new FilesystemException(name, FSCode.UNSUPPORTED_OPERATION);
            }
            @Override public void close() {
                throw new FilesystemException(name, FSCode.UNSUPPORTED_OPERATION);
            }
        };
    }
}