package me.topchetoeu.jscript.utils.filesystem;

public abstract class BaseFile<T> implements File {
    private T handle;
    private Mode mode;

    protected final T handle() {
        return handle;
    }

    protected abstract int onRead(byte[] buff);
    protected abstract void onWrite(byte[] buff);
    protected abstract long onSeek(long offset, int pos);
    protected abstract boolean onClose();

    @Override public int read(byte[] buff) {
        try {
            if (handle == null) throw new FilesystemException(ErrorReason.CLOSED);
            if (!mode.readable) throw new FilesystemException(ErrorReason.NO_PERMISSION, "File not open for reading.");
            return onRead(buff);
        }
        catch (FilesystemException e) { throw e.setAction(ActionType.READ); }
    }
    @Override public void write(byte[] buff) {
        try {
            if (handle == null) throw new FilesystemException(ErrorReason.CLOSED);
            if (!mode.writable) throw new FilesystemException(ErrorReason.NO_PERMISSION, "File not open for writting.");
            onWrite(buff);
        }
        catch (FilesystemException e) { throw e.setAction(ActionType.WRITE); }
    }
    @Override public long seek(long offset, int pos) {
        try {
            if (handle == null) throw new FilesystemException(ErrorReason.CLOSED);
            if (!mode.writable) throw new FilesystemException(ErrorReason.NO_PERMISSION, "File not open for seeking.");
            return onSeek(offset, pos);
        }
        catch (FilesystemException e) { throw e.setAction(ActionType.SEEK); }
    }
    @Override public boolean close() {
        if (handle != null) {
            try {
                var res = onClose();
                handle = null;
                mode = Mode.NONE;
                return res;
            }
            catch (FilesystemException e) { throw e.setAction(ActionType.CLOSE); }
        }
        else return false;
    }

    public BaseFile(T handle, Mode mode) {
        this.mode = mode;
        this.handle = handle;

        if (mode == Mode.NONE) this.handle = null;
    }
}
