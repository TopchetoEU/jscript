package me.topchetoeu.jscript.utils.debug;

import me.topchetoeu.jscript.core.debug.DebugHandler;
import java.io.IOException;

public interface Debugger extends DebugHandler {
    void close();

    void enable(V8Message msg) throws IOException;
    void disable(V8Message msg) throws IOException;

    void setBreakpointByUrl(V8Message msg) throws IOException;
    void removeBreakpoint(V8Message msg) throws IOException;
    void continueToLocation(V8Message msg) throws IOException;

    void getScriptSource(V8Message msg) throws IOException;
    void getPossibleBreakpoints(V8Message msg) throws IOException;

    void resume(V8Message msg) throws IOException;
    void pause(V8Message msg) throws IOException;

    void stepInto(V8Message msg) throws IOException;
    void stepOut(V8Message msg) throws IOException;
    void stepOver(V8Message msg) throws IOException;

    void setPauseOnExceptions(V8Message msg) throws IOException;

    void evaluateOnCallFrame(V8Message msg) throws IOException;

    void getProperties(V8Message msg) throws IOException;
    void releaseObjectGroup(V8Message msg) throws IOException;
    void releaseObject(V8Message msg) throws IOException;
    void callFunctionOn(V8Message msg) throws IOException;

    void runtimeEnable(V8Message msg) throws IOException;
}
