package me.topchetoeu.jscript.engine.debug;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Base64;

import me.topchetoeu.jscript.engine.Engine;
import me.topchetoeu.jscript.engine.debug.WebSocketMessage.Type;
import me.topchetoeu.jscript.engine.debug.handlers.DebuggerHandles;
import me.topchetoeu.jscript.exceptions.SyntaxException;

public class DebugServer {
    public static String browserDisplayName = "jscript";
    public static String targetName = "target";

    public final Engine engine;

    private static void send(Socket socket, String val) throws IOException {
        Http.writeResponse(socket.getOutputStream(), 200, "OK", "application/json", val.getBytes());
    }

    // SILENCE JAVA
    private MessageDigest getDigestInstance() {
        try {
            return MessageDigest.getInstance("sha1");
        }
        catch (Throwable a) { return null; }
    }

    private static Thread runAsync(Runnable func, String name) {
        var res = new Thread(func);
        res.setName(name);
        res.start();
        return res;
    }

    private void handle(WebSocket ws) throws InterruptedException, IOException {
        WebSocketMessage raw;

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

            switch (msg.name) {
                case "Debugger.enable": DebuggerHandles.enable(msg, engine, ws); continue;
                case "Debugger.disable": DebuggerHandles.disable(msg, engine, ws); continue;
                case "Debugger.stepInto": DebuggerHandles.stepInto(msg, engine, ws); continue;
            }
        }
    }
    private void onWsConnect(HttpRequest req, Socket socket) throws IOException {
        var key = req.headers().get("sec-websocket-key");

        if (key == null) {
            Http.writeResponse(
                socket.getOutputStream(), 426, "Upgrade Required", "text/txt",
                "Expected a WS upgrade".getBytes()
            );
            return;
        }

        var resKey = Base64.getEncoder().encodeToString(getDigestInstance().digest(
            (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes()
        ));

        Http.writeCode(socket.getOutputStream(), 101, "Switching Protocols");
        Http.writeHeader(socket.getOutputStream(), "Connection", "Upgrade");
        Http.writeHeader(socket.getOutputStream(), "Sec-WebSocket-Accept", resKey);
        Http.writeLastHeader(socket.getOutputStream(), "Upgrade", "WebSocket");

        var ws = new WebSocket(socket);

        runAsync(() -> {
            try {
                handle(ws);
            }
            catch (InterruptedException e) { return; }
            catch (IOException e) { e.printStackTrace(); }
            finally { ws.close(); }
        }, "Debug Server Message Reader");
        runAsync(() -> {
            try {
                handle(ws);
            }
            catch (InterruptedException e) { return; }
            catch (IOException e) { e.printStackTrace(); }
            finally { ws.close(); }
        }, "Debug Server Event Writer");
    }

    public void open(InetSocketAddress address) throws IOException {
        ServerSocket server = new ServerSocket();
        server.bind(address);

        try {
            while (true) {
                var socket = server.accept();
                var req = Http.readRequest(socket.getInputStream());

                switch (req.path()) {
                    case "/json/version":
                        send(socket, "{\"Browser\":\"" + browserDisplayName + "\",\"Protocol-Version\":\"1.2\"}");
                        break;
                    case "/json/list":
                    case "/json":
                        var addr = "ws://" + address.getHostString() + ":" + address.getPort() + "/devtools/page/" + targetName;
                        send(socket, "{\"id\":\"" + browserDisplayName + "\",\"webSocketDebuggerUrl\":\"" + addr + "\"}");
                        break;
                    case "/json/new":
                    case "/json/activate":
                    case "/json/protocol":
                    case "/json/close":
                    case "/devtools/inspector.html":
                        Http.writeResponse(
                            socket.getOutputStream(),
                            501, "Not Implemented", "text/txt",
                            "This feature isn't (and won't be) implemented.".getBytes()
                        );
                        break;
                    default:
                        if (req.path().equals("/devtools/page/" + targetName)) onWsConnect(req, socket);
                        else {
                            Http.writeResponse(
                                socket.getOutputStream(),
                                404, "Not Found", "text/txt",
                                "Not found :/".getBytes()
                            );
                        }
                        break;
                }
            }
        }
        finally { server.close(); }
    }

    public DebugServer(Engine engine) {
        this.engine = engine;
    }
}
