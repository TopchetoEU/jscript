package me.topchetoeu.jscript.engine.debug;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import me.topchetoeu.jscript.engine.debug.WebSocketMessage.Type;
import me.topchetoeu.jscript.exceptions.UncheckedIOException;

public class WebSocket implements AutoCloseable {
    public long maxLength = 1 << 20;

    private Socket socket;
    private boolean closed = false;

    private OutputStream out() {
        try { return socket.getOutputStream(); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    private InputStream in() {
        try { return socket.getInputStream(); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    private long readLen(int byteLen) {
        long res = 0;

        try {
            if (byteLen == 126) {
                res |= in().read() << 8;
                res |= in().read();
                return res;
            }
            else if (byteLen == 127) {
                res |= in().read() << 56;
                res |= in().read() << 48;
                res |= in().read() << 40;
                res |= in().read() << 32;
                res |= in().read() << 24;
                res |= in().read() << 16;
                res |= in().read() << 8;
                res |= in().read();
                return res;
            }
            else return byteLen;
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    private byte[] readMask(boolean has) {
        if (has) {
            try { return new byte[] {
                (byte)in().read(),
                (byte)in().read(),
                (byte)in().read(),
                (byte)in().read()
            }; }
            catch (IOException e) { throw new UncheckedIOException(e); }
        }
        else return new byte[4];
    }

    private void writeLength(int len) {
        try {
            if (len < 126) {
                out().write((int)len);
            }
            else if (len <= 0xFFFF) {
                out().write(126);
                out().write((int)(len >> 8) & 0xFF);
                out().write((int)len & 0xFF);
            }
            else {
                out().write(127);
                out().write((len >> 56) & 0xFF);
                out().write((len >> 48) & 0xFF);
                out().write((len >> 40) & 0xFF);
                out().write((len >> 32) & 0xFF);
                out().write((len >> 24) & 0xFF);
                out().write((len >> 16) & 0xFF);
                out().write((len >> 8) & 0xFF);
                out().write(len & 0xFF);
            }
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    private synchronized void write(int type, byte[] data) {
        try {
            int i;

            for (i = 0; i < data.length / 0xFFFF; i++) {
                out().write(type);
                writeLength(0xFFFF);
                out().write(data, i * 0xFFFF, 0xFFFF);
                type = 0;
            }

            out().write(type | 0x80);
            writeLength(data.length % 0xFFFF);
            out().write(data, i * 0xFFFF, data.length % 0xFFFF);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void send(String data) {
        if (closed) throw new IllegalStateException("Object is closed.");
        write(1, data.getBytes());
    }
    public void send(byte[] data) {
        if (closed) throw new IllegalStateException("Object is closed.");
        write(2, data);
    }
    public void send(WebSocketMessage msg) {
        if (msg.type == Type.Binary) send(msg.binaryData());
        else send(msg.textData());
    }
    public void send(Object data) {
        // TODO: Remove
        // System.out.println("SEND: " + data);
        if (closed) throw new IllegalStateException("Object is closed.");
        write(1, data.toString().getBytes());
    }

    public void close(String reason) {
        if (socket != null) {
            try {
                write(8, reason.getBytes());
                socket.close();
            }
            catch (Throwable e) { }
        }

        socket = null;
        closed = true;
    }
    public void close() {
        close("");
    }

    private WebSocketMessage fail(String reason) {
        System.out.println("WebSocket Error: " + reason);
        close(reason);
        return null;
    }

    private byte[] readData() {
        try {
            var maskLen = in().read();
            var hasMask = (maskLen & 0x80) != 0;
            var len = (int)readLen(maskLen & 0x7F);
            var mask = readMask(hasMask);
    
            if (len > maxLength) fail("WebSocket Error: client exceeded configured max message size");
            else {
                var buff = new byte[len];
    
                if (in().read(buff) < len) fail("WebSocket Error: payload too short");
                else {
                    for (int i = 0; i < len; i++) {
                        buff[i] ^= mask[(int)(i % 4)];
                    }
                    return buff;
                }
            }

            return null;
        }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public WebSocketMessage receive() {
        try {
            var data = new ByteArrayOutputStream();
            var type = 0;

            while (socket != null && !closed) {
                var finId = in().read();
                if (finId < 0) break;
                var fin = (finId & 0x80) != 0;
                int id = finId & 0x0F;

                if (id == 0x8) { close(); return null; }
                if (id >= 0x8) {
                    if (!fin) return fail("WebSocket Error: client-sent control frame was fragmented");
                    if (id == 0x9) write(0xA, data.toByteArray());
                    continue;
                }

                if (type == 0) type = id;
                if (type == 0) return fail("WebSocket Error: client used opcode 0x00 for first fragment");

                var buff = readData();
                if (buff == null) break;

                if (data.size() + buff.length > maxLength) return fail("WebSocket Error: client exceeded configured max message size");
                data.write(buff);

                if (!fin) continue;
                var raw = data.toByteArray();

                // TODO: Remove
                // System.out.println("RECEIVED: " + new String(raw));


                if (type == 1) return new WebSocketMessage(new String(raw));
                else return new WebSocketMessage(raw);
            }
        }
        catch (IOException e) {
            close();
        }

        return null;
    }

    public WebSocket(Socket socket) {
        this.socket = socket;
    }
}
