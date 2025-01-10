package me.topchetoeu.j2s.runtime;

import me.topchetoeu.j2s.runtime.values.Value;
import me.topchetoeu.j2s.runtime.values.objects.ArrayValue;

public class ArgumentsValue extends ArrayValue {
	public final Frame frame;

	public ArgumentsValue(Frame frame, Value... args) {
		super(args);
		this.frame = frame;
	}
}
