package me.topchetoeu.j2s.lib.debug;

public interface DebuggerProvider {
	Debugger getDebugger(WebSocket socket, HttpRequest req);
}
