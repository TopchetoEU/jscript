package me.topchetoeu.jscript.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import me.topchetoeu.jscript.filesystem.FilesystemException.FSCode;

public class ListFile implements File {
    private Stream<Path> stream;
    private String filename;
    private byte[] currFile;
    private long ptr = 0, start = 0, end = 0;

    private void next() {
        while (stream != null) {
            var opt = stream.findFirst();

            if (opt.isPresent()) {
                start = end;
                currFile = (opt.get().toString() + "\n").getBytes();
                end = start + currFile.length;
            }
            else {
                stream = null;
                currFile = null;
            }
        }
    }

    @Override
    public void close() {
        stream = null;
        currFile = null;
    }

    @Override
    public int read(byte[] buff) {
        if (stream == null) throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_R);
        if (ptr < start) return 0;

        var i = 0;

        while (i < buff.length) {
            while (i > end) next();

            int cpyN = Math.min(currFile.length, buff.length - i);
            System.arraycopy(buff, i, currFile, 0, cpyN);

            i += cpyN;
        }

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

    public ListFile(Path path) throws IOException {
        stream = Files.list(path);
        filename = path.toString();
    }
}
