package me.topchetoeu.jscript.common.json;

public class JSONElement {
    public static enum Type {
        STRING,
        NUMBER,
        BOOLEAN,
        NULL,
        LIST,
        MAP,
    }

    public static final JSONElement NULL = new JSONElement(Type.NULL, null);

    public static JSONElement map(JSONMap val) {
        return new JSONElement(Type.MAP, val);
    }
    public static JSONElement list(JSONList val) {
        return new JSONElement(Type.LIST, val);
    }
    public static JSONElement string(String val) {
        return new JSONElement(Type.STRING, val);
    }
    public static JSONElement number(double val) {
        return new JSONElement(Type.NUMBER, val);
    }
    public static JSONElement bool(boolean val) {
        return new JSONElement(Type.BOOLEAN, val);
    }

    public static JSONElement of(Object val) {
        if (val instanceof JSONMap) return map((JSONMap)val);
        else if (val instanceof JSONList) return list((JSONList)val);
        else if (val instanceof String) return string((String)val);
        else if (val instanceof Boolean) return bool((Boolean)val);
        else if (val instanceof Number) return number(((Number)val).doubleValue());
        else if (val == null) return NULL;
        else throw new IllegalArgumentException("val must be: String, Boolean, Number, JSONList or JSONMap.");
    }

    public final Type type;
    private final Object value;

    public boolean isMap() { return type == Type.MAP; }
    public boolean isList() { return type == Type.LIST; }
    public boolean isString() { return type == Type.STRING; }
    public boolean isNumber() { return type == Type.NUMBER; }
    public boolean isBoolean() { return type == Type.BOOLEAN; }
    public boolean isNull() { return type == Type.NULL; }

    public JSONMap map() {
        if (!isMap()) throw new IllegalStateException("Element is not a map.");
        return (JSONMap)value;
    }
    public JSONList list() {
        if (!isList()) throw new IllegalStateException("Element is not a map.");
        return (JSONList)value;
    }
    public String string() {
        if (!isString()) throw new IllegalStateException("Element is not a string.");
        return (String)value;
    }
    public double number() {
        if (!isNumber()) throw new IllegalStateException("Element is not a number.");
        return (double)value;
    }
    public boolean bool() {
        if (!isBoolean()) throw new IllegalStateException("Element is not a boolean.");
        return (boolean)value;
    }

    @Override
    public String toString() {
        if (isMap()) return "{...}";
        if (isList()) return "[...]";
        if (isString()) return (String)value;
        if (isString()) return (String)value;
        if (isNumber()) return (double)value + "";
        if (isBoolean()) return (boolean)value + "";
        if (isNull()) return "null";
        return "";
    }

    private JSONElement(Type type, Object val) {
        this.type = type;
        this.value = val;
    }
}
