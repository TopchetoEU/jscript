package me.topchetoeu.j2s.repl.debug;

public interface DebuggerProvider {
    Debugger getDebugger(WebSocket socket, HttpRequest req);
}
