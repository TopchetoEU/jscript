package me.topchetoeu.jscript.repl.debug;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.objects.ArrayLikeValue;

public class StackObject extends ArrayLikeValue {
	public final Frame frame;

	@Override public Value get(int i) {
		if (!has(i)) return null;
		return frame.stack[i];
	}
	@Override public boolean set(Environment env, int i, Value val) {
		if (!has(i)) return false;
		frame.stack[i] = val;
		return true;
	}
	@Override public boolean has(int i) {
		return i >= 0 && i < frame.stackPtr;
	}
	@Override public boolean remove(int i) {
		return false;
	}
	@Override public boolean setSize(int val) {
		return false;
	}
	@Override public int size() {
		return frame.stackPtr;
	}
	// @Override public void set(int i, Value val) {
	// }

	public StackObject(Frame frame) {
		super();
		this.frame = frame;
	}
}
