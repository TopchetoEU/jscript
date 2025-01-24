package me.topchetoeu.j2s.runtime.values.functions;

import me.topchetoeu.j2s.common.Environment;
import me.topchetoeu.j2s.common.FunctionBody;
import me.topchetoeu.j2s.runtime.Frame;
import me.topchetoeu.j2s.runtime.values.Value;
import me.topchetoeu.j2s.runtime.values.objects.ObjectValue;

public final class CodeFunction extends FunctionValue {
	public final FunctionBody body;
	public final Value[][] captures;
	public Environment env;

	private Value onCall(Frame frame) {
		frame.onPush();

		try {
			while (true) {
				var res = frame.next(null, null, null);
				if (res != null) return res;
			}
		}
		finally {
			frame.onPop();
		}
	}

	@Override protected Value onApply(Environment env, Value self, Value... args) {
		var frame = new Frame(this.env, false, null, self, args, this);
		var res = onCall(frame);
		return res;
	}
	@Override protected Value onConstruct(Environment env, Value target, Value... args) {
		var self = new ObjectValue();

		var proto = target.getMember(env, "prototype");
		if (proto instanceof ObjectValue) self.setPrototype(env, (ObjectValue)proto);
		else if (proto == Value.NULL) self.setPrototype(env, null);

		var frame = new Frame(this.env, true, target, self, args, this);

		var ret = onCall(frame);

		if (ret == Value.UNDEFINED || ret.isPrimitive()) return self;
		return ret;
	}

	public CodeFunction(Environment env, String name, FunctionBody body, Value[][] captures) {
		super(name, body.length);
		this.captures = captures;
		this.env = env;
		this.body = body;
	}
}
