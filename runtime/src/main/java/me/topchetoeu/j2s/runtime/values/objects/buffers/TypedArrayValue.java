package me.topchetoeu.j2s.runtime.values.objects.buffers;

import java.util.WeakHashMap;

import me.topchetoeu.j2s.common.environment.Environment;
import me.topchetoeu.j2s.runtime.values.Value;
import me.topchetoeu.j2s.runtime.values.objects.ArrayLikeValue;
import me.topchetoeu.j2s.runtime.values.objects.ObjectValue;
import me.topchetoeu.j2s.runtime.values.primitives.UserValue;
import me.topchetoeu.j2s.runtime.values.primitives.numbers.NumberValue;

public abstract class TypedArrayValue extends ArrayLikeValue {
	public static final WeakHashMap<byte[], UserValue<byte[]>> userValues = new WeakHashMap<>();

	public final byte[] buffer;
	public final int elementSize;
	public final int start;
	public final int end;

	protected abstract int onGet(int i);
	protected abstract void onSet(int i, int val);

	@Override public int size() {
		return end - start;
	}

	@Override public boolean setSize(int val) {
		return false;
	}

	@Override public Value get(int i) {
		if (i < 0 || i >= size()) return null;
		return NumberValue.of(onGet(i));
	}
	@Override public boolean set(Environment env, int i, Value val) {
		if (i < 0 || i >= size()) return false;
		onSet(i, val.toNumber(env).getInt());
		return true;
	}
	@Override public boolean has(int i) {
		return i >= 0 && i < size();
	}
	@Override public boolean remove(int i) {
		return false;
	}

	public TypedArrayValue(byte[] buffer, int elementSize, int start, int end) {
		this.buffer = buffer;
		this.elementSize = elementSize;
		this.start = start;
		this.end = end;
	}

	public static UserValue<byte[]> buffer(byte[] buff, ObjectValue proto) {
		if (userValues.containsKey(buff)) return userValues.get(buff);
		var res = UserValue.of(buff, proto);
		userValues.put(buff, res);
		return res;
	}
}
