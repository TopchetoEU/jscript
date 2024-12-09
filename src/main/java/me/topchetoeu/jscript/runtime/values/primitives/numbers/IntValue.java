package me.topchetoeu.jscript.runtime.values.primitives.numbers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;

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

	@Override public int hashCode() {
		return Long.hashCode(value);
	}
	@Override public String toString() { return value + ""; }
	@Override public boolean equals(Object other) {
		if (this == other) return true;
		else if (other instanceof NumberValue val) return val.isLong() && value == val.getLong();
		else return false;
	}

	@Override public List<String> toReadableLines(Environment env, HashSet<ObjectValue> passed) {
		return Arrays.asList(value + "i");
	}

	public IntValue(long value) {
		this.value = value;
	}
	public IntValue(int value) {
		this.value = value;
	}
}
