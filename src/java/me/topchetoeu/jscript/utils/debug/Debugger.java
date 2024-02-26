package me.topchetoeu.jscript.utils.debug;

import me.topchetoeu.jscript.core.debug.DebugController;

public interface Debugger extends DebugHandler, DebugController {
    void close();
}
