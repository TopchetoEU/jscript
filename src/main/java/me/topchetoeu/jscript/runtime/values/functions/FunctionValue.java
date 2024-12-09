package me.topchetoeu.jscript.runtime.values.functions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

public abstract class FunctionValue extends ObjectValue {
	public String name = "";
	public int length;
	public Value prototype = new ObjectValue();

	public boolean enableApply = true;
	public boolean enableConstruct = true;

	private final FieldMember nameField = new FieldMember(this, true, false, false) {
		@Override public Value get(Environment env, Value self) {
			if (name == null) return StringValue.of("");
			return StringValue.of(name);
		}
		@Override public boolean set(Environment env, Value val, Value self) {
			name = val.toString(env);
			return true;
		}
	};
	private final FieldMember lengthField = new FieldMember(this, true, false, false) {
		@Override public Value get(Environment env, Value self) {
			return NumberValue.of(length);
		}
		@Override public boolean set(Environment env, Value val, Value self) {
			return false;
		}
	};
	private final FieldMember prototypeField = new FieldMember(this, false, false, true) {
		@Override public Value get(Environment env, Value self) {
			return prototype;
		}
		@Override public boolean set(Environment env, Value val, Value self) {
			prototype = val;
			return true;
		}
	};

	protected abstract Value onCall(Environment ext, boolean isNew, Value thisArg, Value ...args);

	@Override public String toString() { return String.format("function %s(...)", name); }
	@Override public Value apply(Environment env, Value self, Value... args) {
		if (!enableApply) throw EngineException.ofType("Function cannot be applied");
		return onCall(env, false, self, args);
	}
	@Override public Value construct(Environment env, Value self, Value... args) {
		if (!enableConstruct) throw EngineException.ofType("Function cannot be constructed");
		return onCall(env, true, self, args);
	}

	@Override public Member getOwnMember(Environment env, KeyCache key) {
		switch (key.toString(env)) {
			case "length": return lengthField;
			case "name": return nameField;
			case "prototype": return prototypeField;
			default: return super.getOwnMember(env, key);
		}
	}
	@Override public boolean deleteOwnMember(Environment env, KeyCache key) {
		switch (key.toString(env)) {
			case "length":
				length = 0;
				return true;
			case "name":
				name = "";
				return true;
			case "prototype":
				return false;
			default: return super.deleteOwnMember(env, key);
		}
	}

	@Override public StringValue type() { return StringValue.of("function"); }

	@Override public List<String> toReadableLines(Environment env, HashSet<ObjectValue> passed) {
		var dbg = DebugContext.get(env);
		var res = new StringBuilder(this.toString());
		var loc = dbg.getMapOrEmpty(this).start();

		if (loc != null) res.append(" @ " + loc);

		var lines = new LinkedList<String>(super.toReadableLines(env, passed));
		if (lines.size() == 1 && lines.getFirst().equals("{}")) return Arrays.asList(res.toString());
		lines.set(0, res.toString() + " " + lines.getFirst());

		return lines;
	}

	public void setName(String val) {
		if (this.name == null || this.name.equals("")) this.name = val;
	}

	public FunctionValue(String name, int length) {
		setPrototype(FUNCTION_PROTO);

		if (name == null) name = "";
		this.length = length;
		this.name = name;

		prototype.defineOwnMember(null, "constructor", this);
	}
}

