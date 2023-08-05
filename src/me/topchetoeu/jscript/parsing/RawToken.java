package me.topchetoeu.jscript.parsing;

public class RawToken {
    public final String value;
    public final TokenType type;
    public final int line;
    public final int start;

    public RawToken(String value, TokenType type, int line, int start) {
        this.value = value;
        this.type = type;
        this.line = line;
        this.start = start;
    }
}