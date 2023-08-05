package me.topchetoeu.jscript.engine.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.IllegalFormatException;

// We dont need no http library
public class Http {
    public static void writeCode(OutputStream str, int code, String name) throws IOException {
        str.write(("HTTP/1.1 " + code + " " + name + "\r\n").getBytes());
    }
    public static void writeHeader(OutputStream str, String name, String value) throws IOException {
        str.write((name + ": " + value + "\r\n").getBytes());
    }
    public static void writeLastHeader(OutputStream str, String name, String value) throws IOException {
        str.write((name + ": " + value + "\r\n").getBytes());
        writeHeadersEnd(str);
    }
    public static void writeHeadersEnd(OutputStream str) throws IOException {
        str.write("\n".getBytes());
    }

    public static void writeResponse(OutputStream str, int code, String name, String type, byte[] data) throws IOException {
        writeCode(str, code, name);
        writeHeader(str, "Content-Type", type);
        writeLastHeader(str, "Content-Length", data.length + "");
        str.write(data);
        str.close();
    }

    public static HttpRequest readRequest(InputStream str) throws IOException {
        var lines = new BufferedReader(new InputStreamReader(str));
        var line = lines.readLine();
        var i1 = line.indexOf(" ");
        var i2 = line.lastIndexOf(" ");

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

        return new HttpRequest(method, path, headers);
    }
}
