package me.topchetoeu.jscript.utils.debug;

public interface DebuggerProvider {
    Debugger getDebugger(WebSocket socket, HttpRequest req);
}
