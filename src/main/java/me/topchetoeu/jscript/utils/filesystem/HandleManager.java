package me.topchetoeu.jscript.utils.filesystem;

import java.util.HashSet;
import java.util.Set;

public class HandleManager {
    private Set<File> files = new HashSet<>();

    public File put(File val) {
        var handle = new File() {
            @Override public int read(byte[] buff) {
                return val.read(buff);
            }
            @Override public void write(byte[] buff) {
                val.write(buff);
            }
            @Override public long seek(long offset, int pos) {
                return val.seek(offset, pos);
            }
            @Override public boolean close() {
                return files.remove(this) && val.close();
            }
        };
        files.add(handle);
        return handle;
    }
    public void close() {
        while (!files.isEmpty()) {
            files.stream().findFirst().get().close();
        }
    }
}
