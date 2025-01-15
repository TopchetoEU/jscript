package me.topchetoeu.j2s.repl.buffers;

public final class Uint8ArrayValue extends TypedArrayValue {
	@Override protected int onGet(int i) {
		var res = this.buffer[i + start];
		if (res < 0) res += 0x100;

		return res;
	}
	@Override protected void onSet(int i, int val) {
		if (val > 0x7F) val -= 0x100;
		this.buffer[i + start] = (byte)val;
	}

	public Uint8ArrayValue(byte[] buff, int start, int end) {
		super(buff, 1, start, end);
		setPrototype(UINT8_ARR_PROTO);
	}
}
