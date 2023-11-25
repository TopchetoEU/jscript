package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.filesystem.File;
import me.topchetoeu.jscript.filesystem.FilesystemException;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeGetter;

@Native("File")
public class FileLib {
    public final File file;

    @NativeGetter public PromiseLib pointer(Context ctx) {
        return PromiseLib.await(ctx, () -> {
            try {
                return file.getPtr();
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @NativeGetter public PromiseLib length(Context ctx) {
        return PromiseLib.await(ctx, () -> {
            try {
                long curr = file.getPtr();
                file.setPtr(0, 2);
                long res = file.getPtr();
                file.setPtr(curr, 0);
                return res;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @NativeGetter public PromiseLib getMode(Context ctx) {
        return PromiseLib.await(ctx, () -> {
            try {
                return file.mode().name;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }

    @Native public PromiseLib read(Context ctx, int n) {
        return PromiseLib.await(ctx, () -> {
            try {
                var buff = new byte[n];
                var res = new ArrayValue();
                int resI = file.read(buff);
    
                for (var i = resI - 1; i >= 0; i--) res.set(ctx, i, (int)buff[i]);
                return res;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @Native public PromiseLib write(Context ctx, ArrayValue val) {
        return PromiseLib.await(ctx, () -> {
            try {
                var res = new byte[val.size()];

                for (var i = 0; i < val.size(); i++) res[i] = (byte)Values.toNumber(ctx, val.get(i));
                file.write(res);

                return null;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @Native public PromiseLib close(Context ctx) {
        return PromiseLib.await(ctx, () -> {
            file.close();
            return null;
        });
    }
    @Native public PromiseLib setPointer(Context ctx, long ptr) {
        return PromiseLib.await(ctx, () -> {
            try {
                file.setPtr(ptr, 0);
                return null;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }

    public FileLib(File file) {
        this.file = file;
    }
}
