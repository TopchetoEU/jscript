package me.topchetoeu.jscript.utils.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;

import me.topchetoeu.jscript.common.Buffer;
public interface File {
    default int read(byte[] buff) { throw new FilesystemException(ErrorReason.UNSUPPORTED).setAction(ActionType.READ); }
    default void write(byte[] buff) { throw new FilesystemException(ErrorReason.UNSUPPORTED).setAction(ActionType.WRITE); }
    default long seek(long offset, int pos) { throw new FilesystemException(ErrorReason.UNSUPPORTED).setAction(ActionType.SEEK); }
    default boolean close() { return false; }

    default byte[] readAll() {
        var parts = new LinkedList<byte[]>();
        var sizes = new LinkedList<Integer>();
        var buff = new byte[1024];
        var size = 0;

        while (true) {
            var n = read(buff);
            if (n < 0) break;
            else if (n == 0) continue;

            parts.add(buff);
            sizes.add(n);
            size += n;
            buff = new byte[1024];
        }

        buff = new byte[size];

        var i = 0;
        var j = 0;

        for (var part : parts) {
            var currSize = sizes.get(j++);

            System.arraycopy(part, 0, buff, i, currSize);
            i += currSize;
        }

        return buff;
    }
    default String readToString() {
        return new String(readAll());
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

    public static File ofStream(InputStream str) {
        return new File() {
            @Override public synchronized int read(byte[] buff) {
                try {
                    try { return str.read(buff); }
                    catch (NullPointerException e) { throw new FilesystemException(ErrorReason.ILLEGAL_ARGS, e.getMessage()); }
                    catch (IOException e) { throw new FilesystemException(ErrorReason.UNKNOWN, e.getMessage()); }
                }
                catch (FilesystemException e) { throw e.setAction(ActionType.READ); }
            }
        };
    }
    public static File ofStream(OutputStream str) {
        return new File() {
            @Override public synchronized void write(byte[] buff) {
                try {
                    try { str.write(buff); }
                    catch (NullPointerException e) {throw new FilesystemException(ErrorReason.ILLEGAL_ARGS, e.getMessage()); }
                    catch (IOException e) { throw new FilesystemException(ErrorReason.UNKNOWN, e.getMessage()); }
                }
                catch (FilesystemException e) { throw e.setAction(ActionType.WRITE); }
            }
        };
    }
    public static File ofLineWriter(LineWriter writer) {
        var buff = new Buffer();
        return new File() {
            @Override public synchronized void write(byte[] val) {
                try {
                    if (val == null) throw new FilesystemException(ErrorReason.ILLEGAL_ARGS, "Given buffer is null.");
    
                    for (var b : val) {
                        if (b == '\n') {
                            try {
                                writer.writeLine(new String(buff.data()));
                                buff.clear();
                            }
                            catch (IOException e) {
                                throw new FilesystemException(ErrorReason.UNKNOWN, e.getMessage());
                            }
                        }
                        else buff.append(b);
                    }
                }
                catch (FilesystemException e) { throw e.setAction(ActionType.WRITE); }
            }
        };
    }
    public static File ofLineReader(LineReader reader) {
        return new File() {
            private int offset = 0;
            private byte[] prev = new byte[0];

            @Override
            public synchronized int read(byte[] buff) {
                try {
                    if (buff == null) throw new FilesystemException(ErrorReason.ILLEGAL_ARGS, "Given buffer is null.");
                    var ptr = 0;

                    while (true) {
                        if (prev == null) break;
                        if (offset >= prev.length) {
                            try {
                                var line = reader.readLine();

                                if (line == null) {
                                    prev = null;
                                    break;
                                }
                                else prev = (line + "\n").getBytes();

                                offset = 0;
                            }
                            catch (IOException e) {
                                throw new FilesystemException(ErrorReason.UNKNOWN, e.getMessage());
                            }
                        }

                        if (ptr + prev.length - offset > buff.length) {
                            var n = buff.length - ptr;
                            System.arraycopy(prev, offset, buff, ptr, buff.length - ptr);
                            offset += n;
                            ptr += n;
                            break;
                        }
                        else {
                            var n = prev.length - offset;
                            System.arraycopy(prev, offset, buff, ptr, n);
                            offset += n;
                            ptr += n;
                        }
                    }

                    return ptr;
                }
                catch (FilesystemException e) { throw e.setAction(ActionType.READ); }
            }
        };
    }
    public static File ofIterator(Iterator<String> it) {
        return ofLineReader(LineReader.ofIterator(it));
    }
}