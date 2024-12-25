package me.topchetoeu.jscript.repl.debug;

public class WebSocketMessage {
    public static enum Type {
        Text,
        Binary,
    }

    public final Type type;
    private final Object data;

    public final String textData() {
        if (type != Type.Text) throw new IllegalStateException("Message is not text.");
        return (String)data;
    }
    public final byte[] binaryData() {
        if (type != Type.Binary) throw new IllegalStateException("Message is not binary.");
        return (byte[])data;
    }

    public WebSocketMessage(String data) {
        this.type = Type.Text;
        this.data = data;
    }
    public WebSocketMessage(byte[] data) {
        this.type = Type.Binary;
        this.data = data;
    }
}
