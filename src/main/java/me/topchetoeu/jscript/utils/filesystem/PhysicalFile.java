package me.topchetoeu.jscript.utils.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

public class PhysicalFile extends BaseFile<RandomAccessFile> {
    @Override protected int onRead(byte[] buff) {
        try { return handle().read(buff); }
        catch (IOException e) { throw new FilesystemException(ErrorReason.NO_PERMISSION).setAction(ActionType.READ); }
    }
    @Override protected void onWrite(byte[] buff) {
        try { handle().write(buff); }
        catch (IOException e) { throw new FilesystemException(ErrorReason.NO_PERMISSION).setAction(ActionType.WRITE); }
    }
    @Override protected long onSeek(long offset, int pos) {
        try {
            if (pos == 1) offset += handle().getFilePointer();
            else if (pos == 2) offset += handle().length();
            handle().seek(offset);
            return offset;
        }
        catch (IOException e) { throw new FilesystemException(ErrorReason.NO_PERMISSION).setAction(ActionType.SEEK); }
    }
    @Override protected boolean onClose() {
        try { handle().close(); }
        catch (IOException e) {} // SHUT
        return true;
    }

    public PhysicalFile(Path path, Mode mode) throws FileNotFoundException {
        super(new RandomAccessFile(path.toFile(), mode.name), mode);
    }
}
