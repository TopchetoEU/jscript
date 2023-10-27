package me.topchetoeu.jscript.engine.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;

import me.topchetoeu.jscript.exceptions.UncheckedIOException;

public class HttpRequest {
    public final String method;
    public final String path;
    public final Map<String, String> headers;
    public final OutputStream out;


    public void writeCode(int code, String name) {
        try { out.write(("HTTP/1.1 " + code + " " + name + "\r\n").getBytes()); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public void writeHeader(String name, String value) {
        try { out.write((name + ": " + value + "\r\n").getBytes()); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public void writeLastHeader(String name, String value) {
        try { out.write((name + ": " + value + "\r\n\r\n").getBytes()); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public void writeHeadersEnd() {
        try { out.write("\n".getBytes()); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public void writeResponse(int code, String name, String type, byte[] data) {
        writeCode(code, name);
        writeHeader("Content-Type", type);
        writeLastHeader("Content-Length", data.length + "");
        try {
            out.write(data);
            out.close();
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public void writeResponse(int code, String name, String type, InputStream data) {
        try {
            writeResponse(code, name, type, data.readAllBytes());
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public HttpRequest(String method, String path, Map<String, String> headers, OutputStream out) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.out = out;
    }

    // We dont need no http library
    public static HttpRequest read(Socket socket) {
        try {
            var str = socket.getInputStream();
            var lines = new BufferedReader(new InputStreamReader(str));
            var line = lines.readLine();
            var i1 = line.indexOf(" ");
            var i2 = line.indexOf(" ", i1 + 1);

            if (i1 < 0 || i2 < 0) {
                socket.close();
                return null;
            }

            var method = line.substring(0, i1).trim().toUpperCase();
            var path = line.substring(i1 + 1, i2).trim();
            var headers = new HashMap<String, String>();

            while (!(line = lines.readLine()).isEmpty()) {
                var i = line.indexOf(":");
                if (i < 0) continue;
                var name = line.substring(0, i).trim().toLowerCase();
                var value = line.substring(i + 1).trim();

                if (name.length() == 0) continue;
                headers.put(name, value);
            }

            if (headers.containsKey("content-length")) {
                try {
                    var i = Integer.parseInt(headers.get("content-length"));
                    str.skip(i);
                }
                catch (IllegalFormatException e) { /* ¯\_(ツ)_/¯ */ }
            }

            return new HttpRequest(method, path, headers, socket.getOutputStream());
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
}

