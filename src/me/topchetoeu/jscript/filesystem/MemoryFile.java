package me.topchetoeu.jscript.filesystem;

import me.topchetoeu.jscript.Buffer;
import me.topchetoeu.jscript.filesystem.FilesystemException.FSCode;

public class MemoryFile implements File {
    private int ptr;
    private Mode mode;
    private Buffer data;
    private String filename;

    public Buffer data() { return data; }

    @Override
    public int read(byte[] buff) {
        if (data == null || !mode.readable) throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_R);
        var res = data.read(ptr, buff);
        ptr += res;
        return res;
    }
    @Override
    public void write(byte[] buff) {
        if (data == null || !mode.writable) throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_RW);

        data.write(ptr, buff);
        ptr += buff.length;
    }

    @Override
    public long seek(long offset, int pos) {
        if (data == null || !mode.readable) throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_R);

        if (pos == 0) ptr = (int)offset;
        else if (pos == 1) ptr += (int)offset;
        else if (pos == 2) ptr = data.length() - (int)offset;

        if (ptr < 0) ptr = 0;
        if (ptr > data.length()) ptr = data.length();

        return pos;
    }

    @Override
    public void close() {
        mode = Mode.NONE;
        ptr = 0;
    }

    public MemoryFile(String filename, Buffer buff, Mode mode) {
        this.filename = filename;
        this.data = buff;
        this.mode = mode;
    }

    public static MemoryFile fromFileList(String filename, java.io.File[] list) {
        var res = new StringBuilder();

        for (var el : list) res.append(el.getName()).append('\n');

        return new MemoryFile(filename, new Buffer(res.toString().getBytes()), Mode.READ);
    }
}
