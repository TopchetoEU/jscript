package me.topchetoeu.jscript.core.engine.debug;

public interface DebuggerProvider {
    Debugger getDebugger(WebSocket socket, HttpRequest req);
}
