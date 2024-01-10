package me.topchetoeu.jscript.utils.filesystem;

import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Stream;

import me.topchetoeu.jscript.utils.filesystem.FilesystemException.FSCode;

public class ListFile implements File {
    private Iterator<String> it;
    private String filename;
    private byte[] currFile;
    private long ptr = 0, start = 0, end = 0;

    private void next() {
        if (it != null && it.hasNext()) {
            start = end;
            currFile = (it.next() + "\n").getBytes();
            end = start + currFile.length;
        }
        else {
            it = null;
            currFile = null;
            end = -1;
        }
    }

    @Override
    public void close() {
        it = null;
        currFile = null;
    }

    @Override
    public int read(byte[] buff) {
        if (ptr < start) return 0;
        if (it == null) return 0;

        var i = 0;

        while (i < buff.length) {
            while (i + ptr >= end) {
                next();
                if (it == null) return 0;
            }

            int cpyN = Math.min(currFile.length, buff.length - i);
            System.arraycopy(currFile, (int)(ptr + i - start), buff, i, cpyN);

            i += cpyN;
        }

        ptr += i;
        return i;
    }

    @Override
    public long seek(long offset, int pos) {
        if (pos == 2) throw new FilesystemException(filename, FSCode.UNSUPPORTED_OPERATION);
        if (pos == 1) offset += ptr;
        return ptr = offset;
    }

    @Override
    public void write(byte[] buff) {
        throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_RW);
    }

    public ListFile(String filename, Stream<String> stream) throws IOException {
        this.it = stream.iterator();
        this.filename = filename;
    }
}
