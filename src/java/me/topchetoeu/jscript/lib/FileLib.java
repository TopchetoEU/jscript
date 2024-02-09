package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.core.engine.values.ArrayValue;
import me.topchetoeu.jscript.core.engine.values.Values;
import me.topchetoeu.jscript.utils.filesystem.File;
import me.topchetoeu.jscript.utils.filesystem.FilesystemException;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("File")
public class FileLib {
    public final File fd;

    @Expose public PromiseLib __pointer(Arguments args) {
        return PromiseLib.await(args.ctx, () -> {
            try {
                return fd.seek(0, 1);
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @Expose public PromiseLib __length(Arguments args) {
        return PromiseLib.await(args.ctx, () -> {
            try {
                long curr = fd.seek(0, 1);
                long res = fd.seek(0, 2);
                fd.seek(curr, 0);
                return res;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }

    @Expose public PromiseLib __read(Arguments args) {
        return PromiseLib.await(args.ctx, () -> {
            var n = args.getInt(0);
            try {
                var buff = new byte[n];
                var res = new ArrayValue();
                int resI = fd.read(buff);

                for (var i = resI - 1; i >= 0; i--) res.set(args.ctx, i, (int)buff[i]);
                return res;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @Expose public PromiseLib __write(Arguments args) {
        return PromiseLib.await(args.ctx, () -> {
            var val = args.convert(0, ArrayValue.class);
            try {
                var res = new byte[val.size()];

                for (var i = 0; i < val.size(); i++) res[i] = (byte)Values.toNumber(args.ctx, val.get(i));
                fd.write(res);

                return null;
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }
    @Expose public PromiseLib __close(Arguments args) {
        return PromiseLib.await(args.ctx, () -> {
            fd.close();
            return null;
        });
    }
    @Expose public PromiseLib __seek(Arguments args) {
        return PromiseLib.await(args.ctx, () -> {
            var ptr = args.getLong(0);
            var whence = args.getInt(1);

            try {
                return fd.seek(ptr, whence);
            }
            catch (FilesystemException e) { throw e.toEngineException(); }
        });
    }

    public FileLib(File fd) {
        this.fd = fd;
    }
}
