package me.topchetoeu.j2s.runtime.values.primitives.numbers;

import me.topchetoeu.j2s.common.StringifyUtils;

public final class DoubleValue implements NumberValue {
	public final double value;

	@Override public boolean isInt() {
		return (int)value == value;
	}
	@Override public boolean isLong() {
		return (long)value == value;
	}
	@Override public int getInt() {
		return (int)value;
	}
	@Override public long getLong() {
		return (long)value;
	}
	@Override public double getDouble() {
		return value;
	}

	@Override public String toString() { return StringifyUtils.quoteNumber(value); }

	@Override public int hashCode() {
		return Double.hashCode(value);
	}
	@Override public boolean equals(Object other) {
		if (this == other) return true;
		else if (other instanceof NumberValue val) return value == val.getDouble();
		else return false;
	}

	/**
	 * This constructs a double value directly. In almost all cases, you want to use NumberValue.of instead
	 */
	public DoubleValue(double value) {
		this.value = value;
	}
}
