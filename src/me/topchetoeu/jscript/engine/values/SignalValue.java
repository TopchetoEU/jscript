package me.topchetoeu.jscript.engine.values;

public final class SignalValue {
    public final String data;

    public SignalValue(String data) {
        this.data = data;
    }

    public static boolean isSignal(Object signal, String value) {
        if (!(signal instanceof SignalValue)) return false;
        var val = ((SignalValue)signal).data;

        if (value.endsWith("*")) return val.startsWith(value.substring(0, value.length() - 1));
        else return val.equals(value);
    }
}
