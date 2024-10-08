package me.topchetoeu.jscript.runtime.values.primitives.numbers;

public final class IntValue extends NumberValue {
    public final long value;

    @Override public boolean isInt() {
        return (int)value == value;
    }
    @Override public boolean isLong() {
        return true;
    }
    @Override public int getInt() {
        return (int)value;
    }
    @Override public long getLong() {
        return value;
    }
    @Override public double getDouble() {
        return value;
    }

    @Override public String toString() { return value + ""; }
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        else if (other instanceof NumberValue val) return val.isLong() && value == val.getLong();
        else return false;
    }

    public IntValue(long value) {
        this.value = value;
    }
    public IntValue(int value) {
        this.value = value;
    }
}
