package me.topchetoeu.jscript.runtime;

import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.objects.ArrayValue;

public class ArgumentsValue extends ArrayValue {
	public final Frame frame;

	public ArgumentsValue(Frame frame, Value... args) {
		super(args);
		this.frame = frame;
	}
}
