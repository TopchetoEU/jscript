package me.topchetoeu.jscript.compilation.parsing;

public class Token {
    public final Object value;
    public final String rawValue;
    public final boolean isString;
    public final boolean isRegex;
    public final int line;
    public final int start;

    private Token(int line, int start, Object value, String rawValue, boolean isString, boolean isRegex) {
        this.value = value;
        this.rawValue = rawValue;
        this.line = line;
        this.start = start;
        this.isString = isString;
        this.isRegex = isRegex;
    }
    private Token(int line, int start, Object value, String rawValue) {
        this.value = value;
        this.rawValue = rawValue;
        this.line = line;
        this.start = start;
        this.isString = false;
        this.isRegex = false;
    }

    public boolean isString() { return isString; }
    public boolean isRegex() { return isRegex; }
    public boolean isNumber() { return value instanceof Number; }
    public boolean isIdentifier() { return !isString && !isRegex && value instanceof String; }
    public boolean isOperator() { return value instanceof Operator; }

    public boolean isIdentifier(String lit) { return !isString && !isRegex && value.equals(lit); }
    public boolean isOperator(Operator op) { return value.equals(op); }

    public String string() { return (String)value; }
    public String regex() { return (String)value; }
    public double number() { return (double)value; }
    public String identifier() { return (String)value; }
    public Operator operator() { return (Operator)value; }

    public static Token regex(int line, int start, String val, String rawValue) {
        return new Token(line, start, val, rawValue, false, true);
    }
    public static Token string(int line, int start, String val, String rawValue) {
        return new Token(line, start, val, rawValue, true, false);
    }
    public static Token number(int line, int start, double val, String rawValue) {
        return new Token(line, start, val, rawValue);
    }
    public static Token identifier(int line, int start, String val) {
        return new Token(line, start, val, val);
    }
    public static Token operator(int line, int start, Operator val) {
        return new Token(line, start, val, val.readable);
    }
}