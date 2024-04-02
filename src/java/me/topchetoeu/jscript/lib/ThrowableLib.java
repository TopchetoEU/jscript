package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;

public class ThrowableLib {
    @Expose public static String __message(Arguments args) {
        if (args.self instanceof Throwable) return ((Throwable)args.self).getMessage();
        else return null;
    }
    @Expose public static String __name(Arguments args) {
        return args.self.getClass().getSimpleName();
    }

    @Expose public static String __toString(Arguments args) {
        return __name(args) + ": " + __message(args);
    }
}
