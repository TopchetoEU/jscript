package me.topchetoeu.jscript.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import me.topchetoeu.jscript.filesystem.FilesystemException.FSCode;

public class PhysicalFile implements File {
    private String filename;
    private RandomAccessFile file;
    private Mode perms;

    @Override
    public int read(byte[] buff) {
        if (file == null || !perms.readable) throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_R);
        else try { return file.read(buff); }
        catch (IOException e) { throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_R); }
    }
    @Override
    public void write(byte[] buff) {
        if (file == null || !perms.writable) throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_RW);
        else try { file.write(buff); }
        catch (IOException e) { throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_RW); }
    }

    @Override
    public long getPtr() {
        if (file == null || !perms.readable) throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_R);
        else try { return file.getFilePointer(); }
        catch (IOException e) { throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_R); }
    }
    @Override
    public void setPtr(long offset, int pos) {
        if (file == null || !perms.readable) throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_R);

        try {
            if (pos == 1) pos += file.getFilePointer();
            else if (pos == 2) pos += file.length();
            file.seek(pos);
        }
        catch (IOException e) { throw new FilesystemException(filename, FSCode.NO_PERMISSIONS_R); }
    }

    @Override
    public void close() {
        if (file == null) return;
        try { file.close(); }
        catch (IOException e) {} // SHUT
        file = null;
        perms = Mode.NONE;
    }
    @Override
    public Mode mode() { return perms; }

    public PhysicalFile(String path, Mode mode) throws FileNotFoundException {
        if (mode == Mode.NONE) file = null;
        else try { file = new RandomAccessFile(path, mode.name); }
        catch (FileNotFoundException e) { throw new FilesystemException(filename, FSCode.DOESNT_EXIST); }

        perms = mode;
    }
}
