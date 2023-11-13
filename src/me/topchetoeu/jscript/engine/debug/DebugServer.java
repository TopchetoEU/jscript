package me.topchetoeu.jscript.engine.debug;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;

import me.topchetoeu.jscript.Metadata;
import me.topchetoeu.jscript.engine.debug.WebSocketMessage.Type;
import me.topchetoeu.jscript.events.Notifier;
import me.topchetoeu.jscript.exceptions.SyntaxException;
import me.topchetoeu.jscript.exceptions.UncheckedException;
import me.topchetoeu.jscript.exceptions.UncheckedIOException;
import me.topchetoeu.jscript.json.JSON;
import me.topchetoeu.jscript.json.JSONList;
import me.topchetoeu.jscript.json.JSONMap;

public class DebugServer {
    public static String browserDisplayName = Metadata.name() + "/" + Metadata.version();

    public final HashMap<String, DebuggerProvider> targets = new HashMap<>();

    private final byte[] favicon, index, protocol;
    private final Notifier connNotifier = new Notifier();

    private static void send(HttpRequest req, String val) throws IOException {
        req.writeResponse(200, "OK", "application/json", val.getBytes());
    }

    // SILENCE JAVA
    private MessageDigest getDigestInstance() {
        try {
            return MessageDigest.getInstance("sha1");
        }
        catch (Throwable e) { throw new UncheckedException(e); }

    }

    private static Thread runAsync(Runnable func, String name) {
        var res = new Thread(func);
        res.setName(name);
        res.start();
        return res;
    }

    private void handle(WebSocket ws, Debugger debugger) {
        WebSocketMessage raw;

        debugger.connect();

        while ((raw = ws.receive()) != null) {
            if (raw.type != Type.Text) {
                ws.send(new V8Error("Expected a text message."));
                continue;
            }

            V8Message msg;

            try {
                msg = new V8Message(raw.textData());
            }
            catch (SyntaxException e) {
                ws.send(new V8Error(e.getMessage()));
                return;
            }

            try {
                switch (msg.name) {
                    case "Debugger.enable":
                        connNotifier.next();
                        debugger.enable(msg); continue;
                    case "Debugger.disable": debugger.disable(msg); continue;

                    case "Debugger.setBreakpoint": debugger.setBreakpoint(msg); continue;
                    case "Debugger.setBreakpointByUrl": debugger.setBreakpointByUrl(msg); continue;
                    case "Debugger.removeBreakpoint": debugger.removeBreakpoint(msg); continue;
                    case "Debugger.continueToLocation": debugger.continueToLocation(msg); continue;

                    case "Debugger.getScriptSource": debugger.getScriptSource(msg); continue;
                    case "Debugger.getPossibleBreakpoints": debugger.getPossibleBreakpoints(msg); continue;

                    case "Debugger.resume": debugger.resume(msg); continue;
                    case "Debugger.pause": debugger.pause(msg); continue;

                    case "Debugger.stepInto": debugger.stepInto(msg); continue;
                    case "Debugger.stepOut": debugger.stepOut(msg); continue;
                    case "Debugger.stepOver": debugger.stepOver(msg); continue;

                    case "Debugger.setPauseOnExceptions": debugger.setPauseOnExceptions(msg); continue;
                    case "Debugger.evaluateOnCallFrame": debugger.evaluateOnCallFrame(msg); continue;

                    case "Runtime.releaseObjectGroup": debugger.releaseObjectGroup(msg); continue;
                    case "Runtime.releaseObject": debugger.releaseObject(msg); continue;
                    case "Runtime.getProperties": debugger.getProperties(msg); continue;
                    case "Runtime.callFunctionOn": debugger.callFunctionOn(msg); continue;
                    // case "NodeWorker.enable": debugger.nodeWorkerEnable(msg); continue;
                    case "Runtime.enable": debugger.runtimeEnable(msg); continue;
                }

                if (
                    msg.name.startsWith("DOM.") ||
                    msg.name.startsWith("DOMDebugger.") ||
                    msg.name.startsWith("Emulation.") ||
                    msg.name.startsWith("Input.") ||
                    msg.name.startsWith("Network.") ||
                    msg.name.startsWith("Page.")
                ) ws.send(new V8Error("This isn't a browser..."));
                else ws.send(new V8Error("This API is not supported yet."));
            }
            catch (Throwable e) {
                e.printStackTrace();
                throw new UncheckedException(e);
            }
        }

        debugger.disconnect();
    }
    private void onWsConnect(HttpRequest req, Socket socket, DebuggerProvider debuggerProvider) {
        var key = req.headers.get("sec-websocket-key");

        if (key == null) {
            req.writeResponse(
                426, "Upgrade Required", "text/txt",
                "Expected a WS upgrade".getBytes()
            );
            return;
        }

        var resKey = Base64.getEncoder().encodeToString(getDigestInstance().digest(
            (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes()
        ));

        req.writeCode(101, "Switching Protocols");
        req.writeHeader("Connection", "Upgrade");
        req.writeHeader("Sec-WebSocket-Accept", resKey);
        req.writeLastHeader("Upgrade", "WebSocket");

        var ws = new WebSocket(socket);
        var debugger = debuggerProvider.getDebugger(ws, req);

        if (debugger == null) {
            ws.close();
            return;
        }

        runAsync(() -> {
            try { handle(ws, debugger); }
            catch (RuntimeException e) {
                ws.send(new V8Error(e.getMessage()));
            }
            finally { ws.close(); debugger.disconnect(); }
        }, "Debug Handler");
    }

    public void awaitConnection() {
        connNotifier.await();
    }

    public void run(InetSocketAddress address) {
        try {
            ServerSocket server = new ServerSocket();
            server.bind(address);

            try {
                while (true) {
                    var socket = server.accept();
                    var req = HttpRequest.read(socket);

                    if (req == null) continue;
    
                    switch (req.path) {
                        case "/json/version":
                            send(req, "{\"Browser\":\"" + browserDisplayName + "\",\"Protocol-Version\":\"1.1\"}");
                            break;
                        case "/json/list":
                        case "/json": {
                            var res = new JSONList();

                            for (var el : targets.entrySet()) {
                                res.add(new JSONMap()
                                    .set("description", "JScript debugger")
                                    .set("favicon", "/favicon.ico")
                                    .set("id", el.getKey())
                                    .set("type", "node")
                                    .set("webSocketDebuggerUrl", "ws://" + address.getHostString() + ":" + address.getPort() + "/" + el.getKey())
                                );
                            }
                            send(req, JSON.stringify(res));
                            break;
                        }
                        case "/json/protocol":
                            req.writeResponse(200, "OK", "application/json", protocol);
                            break;
                        case "/json/new":
                        case "/json/activate":
                        case "/json/close":
                        case "/devtools/inspector.html":
                            req.writeResponse(
                                501, "Not Implemented", "text/txt",
                                "This feature isn't (and probably won't be) implemented.".getBytes()
                            );
                            break;
                        case "/":
                        case "/index.html":
                            req.writeResponse(200, "OK", "text/html", index);
                            break;
                        case "/favicon.ico":
                            req.writeResponse(200, "OK", "image/png", favicon);
                            break;
                        default:
                            if (req.path.length() > 1 && targets.containsKey(req.path.substring(1))) {
                                onWsConnect(req, socket, targets.get(req.path.substring(1)));
                            }
                            break;
                    }
                }
            }
            finally { server.close(); }
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public Thread start(InetSocketAddress address, boolean daemon) {
        var res = new Thread(() -> run(address), "Debug Server");
        res.setDaemon(daemon);
        res.start();
        return res;
    }

    public DebugServer() {
        try {
            this.favicon = getClass().getClassLoader().getResourceAsStream("assets/favicon.png").readAllBytes();
            this.protocol = getClass().getClassLoader().getResourceAsStream("assets/protocol.json").readAllBytes();
            var index = new String(getClass().getClassLoader().getResourceAsStream("assets/index.html").readAllBytes());
            this.index = index
                .replace("${NAME}", Metadata.name())
                .replace("${VERSION}", Metadata.version())
                .replace("${AUTHOR}", Metadata.author())
                .getBytes();
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
