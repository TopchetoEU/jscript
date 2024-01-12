package me.topchetoeu.jscript.lib;

import java.io.IOException;
import java.io.OutputStream;

import me.topchetoeu.jscript.core.engine.values.Values;
import me.topchetoeu.jscript.utils.filesystem.FilesystemException;
import me.topchetoeu.jscript.utils.filesystem.FilesystemException.FSCode;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("Console")
public class ConsoleLib {
    private final OutputStream stream;

    @Expose
    public void __log(Arguments args) {
        try {
            var first = true;
            for (var el : args.args) {
                if (!first) stream.write(" ".getBytes());
                first = false;
                stream.write(Values.toReadable(args.ctx, el).getBytes());
            }
            stream.write((byte)'\n');
        }
        catch (IOException e) {
            throw new FilesystemException("stdout", FSCode.NO_PERMISSIONS_RW);
        }
    }

    public ConsoleLib(OutputStream stream) {
        this.stream = stream;
    }
}
