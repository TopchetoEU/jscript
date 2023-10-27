package me.topchetoeu.jscript.engine.debug;

public interface DebugHandler {
    void enable(V8Message msg);
    void disable(V8Message msg);

    void setBreakpoint(V8Message msg);
    void setBreakpointByUrl(V8Message msg);
    void removeBreakpoint(V8Message msg);
    void continueToLocation(V8Message msg);

    void getScriptSource(V8Message msg);
    void getPossibleBreakpoints(V8Message msg);

    void resume(V8Message msg);
    void pause(V8Message msg);

    void stepInto(V8Message msg);
    void stepOut(V8Message msg);
    void stepOver(V8Message msg);

    void setPauseOnExceptions(V8Message msg);
}
