package me.topchetoeu.jscript.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import me.topchetoeu.jscript.filesystem.FilesystemException.FSCode;

public class PhysicalFile implements File {
    private String filename;
    private RandomAccessFile file;
    private Mode mode;

    @Override
    public int read(byte[] buff) {
        if (file == null || !mode.readable) throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_R);
        else try { return file.read(buff); }
        catch (IOException e) { throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_R); }
    }
    @Override
    public void write(byte[] buff) {
        if (file == null || !mode.writable) throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_RW);
        else try { file.write(buff); }
        catch (IOException e) { throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_RW); }
    }

    @Override
    public long seek(long offset, int pos) {
        if (file == null || !mode.readable) throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_R);

        try {
            if (pos == 1) offset += file.getFilePointer();
            else if (pos == 2) offset += file.length();
            file.seek(offset);
            return offset;
        }
        catch (IOException e) { throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_R); }
    }

    @Override
    public void close() {
        if (file == null) return;
        try { file.close(); }
        catch (IOException e) {} // SHUT
        file = null;
        mode = Mode.NONE;
    }

    public PhysicalFile(String name, String path, Mode mode) throws FileNotFoundException {
        this.filename = name;
        this.mode = mode;

        if (mode == Mode.NONE) file = null;
        else try { file = new RandomAccessFile(path, mode.name); }
        catch (FileNotFoundException e) { throw new FilesystemException(filename, FSCode.DOESNT_EXIST); }
    }
}
