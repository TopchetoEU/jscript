package me.topchetoeu.j2s.runtime.values.primitives.numbers;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.runtime.values.objects.ObjectValue;
import me.topchetoeu.j2s.runtime.values.primitives.PrimitiveValue;
import me.topchetoeu.j2s.runtime.values.primitives.StringValue;

public interface NumberValue extends PrimitiveValue {
	public static final NumberValue NAN = new DoubleValue(Double.NaN);

	@Override public default StringValue type() { return StringValue.of("number"); }

	public abstract double getDouble();
	public abstract int getInt();
	public abstract long getLong();

	public abstract boolean isLong();
	public abstract boolean isInt();

	public abstract boolean equals(Object other);
	public abstract String toString();


	@Override public default boolean toBoolean() { return getDouble() != 0; }
	@Override public default NumberValue toNumber(Environment ext) { return this; }
	@Override public default String toString(Environment ext) { return toString(); }

	@Override public default ObjectValue getPrototype(Environment env) {
		return env.get(NUMBER_PROTO);
	}

	public static NumberValue of(double value) {
		if (Double.isNaN(value)) return NAN;
		else if ((int)value == value) return new IntValue((int)value);
		else return new DoubleValue(value);
	}
	public static NumberValue of(long value) {
		return new IntValue(value);
	}
	public static NumberValue of(int value) {
		return new IntValue(value);
	}
}
