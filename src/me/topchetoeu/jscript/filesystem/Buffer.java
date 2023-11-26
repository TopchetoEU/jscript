package me.topchetoeu.jscript.filesystem;

public class Buffer {
    private byte[] data;
    private int length;

    public void write(int i, byte[] val) {
        if (i + val.length > data.length) {
            var newCap = i + val.length + 1;
            if (newCap < data.length * 2) newCap = data.length * 2;

            var tmp = new byte[newCap];
            System.arraycopy(this.data, 0, tmp, 0, length);
            this.data = tmp;
        }

        System.arraycopy(val, 0, data, i, val.length);
        if (i + val.length > length) length = i + val.length;
    }
    public int read(int i, byte[] buff) {
        int n = buff.length;
        if (i + n > length) n = length - i;
        System.arraycopy(data, i, buff, 0, n);
        return n;
    }

    public void append(byte b) {
        write(length, new byte[] { b });
    }

    public byte[] data() {
        var res = new byte[length];
        System.arraycopy(this.data, 0, res, 0, length);
        return res;
    }
    public int length() {
        return length;
    }

    public Buffer(byte[] data) {
        this.data = new byte[data.length];
        this.length = data.length;
        System.arraycopy(data, 0, this.data, 0, data.length);
    }
    public Buffer(int capacity) {
        this.data = new byte[capacity];
        this.length = 0;
    }
    public Buffer() {
        this.data = new byte[128];
        this.length = 0;
    }
}
