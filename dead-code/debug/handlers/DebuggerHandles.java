package me.topchetoeu.jscript.engine.debug.handlers;

import java.io.IOException;

import me.topchetoeu.jscript.engine.DebugCommand;
import me.topchetoeu.jscript.engine.Engine;
import me.topchetoeu.jscript.engine.debug.V8Error;
import me.topchetoeu.jscript.engine.debug.V8Message;
import me.topchetoeu.jscript.engine.debug.WebSocket;
import me.topchetoeu.jscript.json.JSONMap;

public class DebuggerHandles {
    public static void enable(V8Message msg, Engine engine, WebSocket ws) throws IOException {
        if (engine.debugState == null) ws.send(new V8Error("Debugging is disabled for this engine."));
        else ws.send(msg.respond(new JSONMap().set("debuggerId", 1)));
    }
    public static void disable(V8Message msg, Engine engine, WebSocket ws) throws IOException {
        if (engine.debugState == null) ws.send(msg.respond());
        else ws.send(new V8Error("Debugger may not be disabled."));
    }
    public static void stepInto(V8Message msg, Engine engine, WebSocket ws) throws IOException {
        if (engine.debugState == null) ws.send(new V8Error("Debugging is disabled for this engine."));
        else if (!engine.debugState.paused()) ws.send(new V8Error("Debugger is not paused."));
        else {
            engine.debugState.resume(DebugCommand.STEP_INTO);
            ws.send(msg.respond());
        }
    }
}
