package me.topchetoeu.j2s.runtime.values.objects.buffers;

public final class Int8ArrayValue extends TypedArrayValue {
	@Override protected int onGet(int i) {
		return this.buffer[i + start];
	}
	@Override protected void onSet(int i, int val) {
		this.buffer[i + start] = (byte)val;
	}

	public Int8ArrayValue(byte[] buff, int start, int end) {
		super(buff, 1, start, end);
		setPrototype(INT8_ARR_PROTO);
	}
}
