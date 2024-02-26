package me.topchetoeu.jscript.lib;

import java.io.IOException;

import me.topchetoeu.jscript.core.values.Values;
import me.topchetoeu.jscript.utils.filesystem.File;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("Console")
public class ConsoleLib {
    public static interface Writer {
        void writeLine(String val) throws IOException;
    }

    private File file;

    @Expose
    public void __log(Arguments args) {
        var res = new StringBuilder();
        var first = true;
        
        for (var el : args.args) {
            if (!first) res.append(" ");
            first = false;
            res.append(Values.toReadable(args.ctx, el).getBytes());
        }

        for (var line : res.toString().split("\n", -1)) {
            file.write(line.getBytes());
        }
    }

    public ConsoleLib(File file) {
        this.file = file;
    }
}
