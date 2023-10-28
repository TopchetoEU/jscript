package me.topchetoeu.jscript.engine.debug;

public interface DebuggerProvider {
    Debugger getDebugger(WebSocket socket, HttpRequest req);
}
