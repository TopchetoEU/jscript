package me.topchetoeu.j2s.runtime;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.runtime.values.Value;
import me.topchetoeu.j2s.runtime.values.objects.ArrayLikeValue;

public class ArgumentsValue extends ArrayLikeValue {
	public final Frame frame;
	private Value[] args;
	private boolean shadowed;

	private void shadow() {
		if (!shadowed) {
			var newArgs = new Value[args.length];
			System.arraycopy(args, 0, newArgs, 0, args.length);
			args = newArgs;
			shadowed = true;
		}
	}

	@Override public Value get(int i) {
		return args[i];
	}
	@Override public boolean has(int i) {
		if (i < 0 || i >= size()) return false;

		if (shadowed) return args[i] != null;
		else return true;
	}
	@Override public boolean remove(int i) {
		shadow();
		args[i] = null;
		return true;
	}
	@Override public boolean set(Environment env, int i, Value val) {
		shadow();
		args[i] = val;
		return true;
	}
	@Override public boolean setSize(int val) {
		return false;
	}
	@Override public int size() {
		return args.length;
	}

	public ArgumentsValue(Frame frame) {
		this.frame = frame;
		this.args = frame.args;
		this.shadowed = false;
		setPrototype(e -> null);
	}
}
