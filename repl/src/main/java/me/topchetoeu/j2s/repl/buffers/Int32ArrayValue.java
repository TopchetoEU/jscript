package me.topchetoeu.j2s.repl.buffers;

public final class Int32ArrayValue extends TypedArrayValue {
	@Override protected int onGet(int i) {
		i = (i + start) << 2;
		return (
			this.buffer[i] |
			this.buffer[i + 1] << 8 |
			this.buffer[i + 2] << 16 |
			this.buffer[i + 3] << 24
		);
	}
	@Override protected void onSet(int i, int val) {
		i = (i + start) << 2;
		this.buffer[i + start + 0] = (byte)(val & 0xFF);
		this.buffer[i + start + 1] = (byte)(val >> 8 & 0xFF);
		this.buffer[i + start + 2] = (byte)(val >> 16 & 0xFF);
		this.buffer[i + start + 3] = (byte)(val >> 24 & 0xFF);
	}

	public Int32ArrayValue(byte[] buff, int start, int end) {
		super(buff, 4, start, end);
		setPrototype(INT32_ARR_PROTO);
	}
}
