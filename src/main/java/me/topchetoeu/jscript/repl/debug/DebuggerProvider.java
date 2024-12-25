package me.topchetoeu.jscript.repl.debug;

public interface DebuggerProvider {
    Debugger getDebugger(WebSocket socket, HttpRequest req);
}
