package me.topchetoeu.jscript.runtime.values.objects;

import java.util.Arrays;
import java.util.Iterator;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

public class ByteBufferValue extends ArrayLikeValue implements Iterable<Value> {
	public final byte[] values;

	public int size() { return values.length; }
	public boolean setSize(int val) { return false; }

	@Override public Value get(int i) {
		if (i < 0 || i >= values.length) return null;
		return NumberValue.of(values[i]);
	}
	@Override public boolean set(Environment env, int i, Value val) {
		if (i < 0 || i >= values.length) return false;
		values[i] = (byte)val.toNumber(env).getInt();
		return true;
	}
	@Override public boolean has(int i) {
		return i >= 0 && i < values.length;
	}
	@Override public boolean remove(int i) {
		return false;
	}

	public void copyTo(byte[] arr, int sourceStart, int destStart, int count) {
		System.arraycopy(values, sourceStart, arr, destStart, count);
	}
	public void copyTo(ByteBufferValue arr, int sourceStart, int destStart, int count) {
		arr.copyFrom(values, sourceStart, destStart, count);
	}
	public void copyFrom(byte[] arr, int sourceStart, int destStart, int count) {
		System.arraycopy(arr, sourceStart, arr, destStart, count);
	}

	public void move(int srcI, int dstI, int n) {
		System.arraycopy(values, srcI, values, dstI, n);
	}

	public void sort() {
		var buckets = new int[256];

		for (var i = 0; i < values.length; i++) {
			buckets[values[i] + 128]++;
		}

		var offset = 0;

		for (var i = 0; i < values.length; i++) {
			Arrays.fill(values, offset, offset += buckets[i], (byte)(i - 128));
		}
	}

	@Override public Iterator<Value> iterator() {
		return new Iterator<>() {
			private int i = 0;

			@Override public boolean hasNext() {
				return i < size();
			}
			@Override public Value next() {
				if (!hasNext()) return null;
				return get(i++);
			}
		};
	}

	public ByteBufferValue(int size) {
		this(new byte[size]);
	}
	public ByteBufferValue(byte[] buffer) {
		setPrototype(BYTE_BUFF_PROTO);
		this.values = buffer;
	}
}
