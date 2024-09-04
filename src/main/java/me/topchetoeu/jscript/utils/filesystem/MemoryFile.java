package me.topchetoeu.jscript.utils.filesystem;

import me.topchetoeu.jscript.common.Buffer;

class MemoryFile extends BaseFile<Buffer> {
    private int ptr;

    @Override protected int onRead(byte[] buff) {
        if (ptr >= handle().length()) return -1;
        var res = handle().read(ptr, buff);
        ptr += res;
        return res;
    }
    @Override protected void onWrite(byte[] buff) {
        handle().write(ptr, buff);
        ptr += buff.length;
    }
    @Override protected long onSeek(long offset, int pos) {
        if (pos == 0) ptr = (int)offset;
        else if (pos == 1) ptr += (int)offset;
        else if (pos == 2) ptr = handle().length() - (int)offset;

        if (ptr < 0) ptr = 0;
        if (ptr > handle().length()) ptr = handle().length();

        return pos;
    }
    @Override protected boolean onClose() {
        ptr = 0;
        return true;
    }

    public MemoryFile(Buffer buff, Mode mode) {
        super(buff, mode);
    }
}