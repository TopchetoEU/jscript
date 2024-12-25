package me.topchetoeu.jscript.repl.debug;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.Frame;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.SymbolValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

public class ScopeObject extends Value {
	public static final class ScopeMember extends FieldMember {
		public final Frame frame;
		public final int i;

		@Override public Value get(Environment env, Value self) {
			return frame.getVar(i);
		}

		@Override public boolean set(Environment env, Value val, Value self) {
			frame.setVar(i, val);
			return true;
		}

		public ScopeMember(Value self, Frame frame, int i) {
			super(self, false, true, true);

			this.frame = frame;
			this.i = i;
		}
	}

	private final Map<String, FieldMember> fields = new HashMap<>();
	public final ObjectValue proto;

	@Override public StringValue type() {
		return StringValue.of("object");
	}
	@Override public boolean isPrimitive() {
		return false;
	}
	@Override public Value toPrimitive(Environment env) {
		throw EngineException.ofType("Value couldn't be converted to a primitive.");
	}
	@Override public NumberValue toNumber(Environment env) {
		return NumberValue.NAN;
	}
	@Override public String toString(Environment env) {
		return "[Scope]";
	}
	@Override public boolean toBoolean() {
		return true;
	}
	@Override public Member getOwnMember(Environment env, KeyCache key) {
		if (key.isSymbol()) return null;
		var strKey = key.toString(env);
		return fields.get(strKey);
	}
	@Override public Set<String> getOwnMembers(Environment env, boolean onlyEnumerable) {
		return fields.keySet();
	}
	@Override public Set<SymbolValue> getOwnSymbolMembers(Environment env, boolean onlyEnumerable) {
		return new HashSet<>();
	}
	@Override public boolean defineOwnField(Environment env, KeyCache key, Value val, Boolean writable, Boolean enumerable, Boolean configurable) {
		if (key.isSymbol()) return false;
		var strKey = key.toString(env);
		var field = fields.get(strKey);
		if (field == null) return false;
		return field.reconfigure(env, this, val, writable, enumerable, configurable);
	}
	@Override public boolean defineOwnProperty(Environment env, KeyCache key, Optional<FunctionValue> get, Optional<FunctionValue> set, Boolean enumerable, Boolean configurable) {
		return false;
	}
	@Override public boolean deleteOwnMember(Environment env, KeyCache key) {
		return key.isSymbol() || !fields.containsKey(key.toString(env));
	}
	@Override public boolean setPrototype(Environment env, ObjectValue val) {
		return false;
	}
	@Override public State getState() {
		return State.SEALED;
	}

	@Override public void preventExtensions() { }
	@Override public void seal() { }
	@Override public void freeze() { }

	@Override public ObjectValue getPrototype(Environment env) {
		return proto;
	}

	public void add(String name, Value val) {
		fields.put(name, FieldMember.of(this, val, false));
	}
	public void remove(String name) {
		fields.remove(name);
	}

	private ScopeObject(ObjectValue proto) {
		this.proto = proto;
	}
	public ScopeObject(Frame frame, String[] names, IntUnaryOperator transformer, ObjectValue proto) {
		this.proto = proto;

		for (var i = 0; i < names.length; i++) {
			fields.put(names[i], new ScopeMember(this, frame, transformer.applyAsInt(i)));
		}
	}

	private static String[] fixCaptures(Frame frame, String[] names) {
		if (names == null) {
			names = new String[frame.captures.length];
			for (var i = 0; i < names.length; i++) {
				names[i] = "var_" + i;
			}
		}
		else if (names.length > frame.captures.length) {
			var newNames = new String[frame.captures.length];
			System.arraycopy(names, 0, newNames, 0, frame.captures.length);
			names = newNames;
		}
		else if (names.length < frame.captures.length) {
			var newNames = new String[frame.captures.length];
			System.arraycopy(names, 0, newNames, 0, names.length);
			for (var i = names.length; i < frame.captures.length; i++) {
				names[i] = "cap_" + i;
			}
			names = newNames;
		}

		return names;
	}
	private static String[] fixLocals(Frame frame, String[] names) {
		if (names == null) {
			names = new String[frame.locals.length];
			for (var i = 0; i < names.length; i++) {
				names[i] = "var_" + i;
			}
		}
		else if (names.length > frame.locals.length) {
			var newNames = new String[frame.locals.length];
			System.arraycopy(names, 0, newNames, 0, frame.locals.length);
			names = newNames;
		}
		else if (names.length < frame.locals.length) {
			var newNames = new String[frame.locals.length];
			System.arraycopy(names, 0, newNames, 0, names.length);
			for (var i = names.length; i < frame.locals.length; i++) {
				names[i] = "var_" + i;
			}
			names = newNames;
		}

		return names;
	}
	private static String[] fixCapturables(Frame frame, String[] names) {
		if (names == null) {
			names = new String[frame.capturables.length];
			for (var i = 0; i < names.length; i++) {
				names[i] = "var_" + (frame.locals.length + i);
			}
		}
		else if (names.length > frame.capturables.length) {
			var newNames = new String[frame.capturables.length];
			System.arraycopy(names, 0, newNames, 0, frame.capturables.length);
			names = newNames;
		}
		else if (names.length < frame.capturables.length) {
			var newNames = new String[frame.capturables.length];
			System.arraycopy(names, 0, newNames, 0, names.length);
			for (var i = names.length; i < frame.capturables.length; i++) {
				names[i] = "var_" + (frame.locals.length + i);
			}
			names = newNames;
		}

		return names;
	}

	public static ScopeObject locals(Frame frame, String[] names) {
		return new ScopeObject(frame, fixLocals(frame, names), v -> v, null);
	}
	public static ScopeObject capturables(Frame frame, String[] names) {
		return new ScopeObject(frame, fixCapturables(frame, names), v -> v + frame.locals.length, null);
	}
	public static ScopeObject captures(Frame frame, String[] names) {
		return new ScopeObject(frame, fixCaptures(frame, names), v -> ~v, null);
	}

	public static ScopeObject combine(ObjectValue proto, ScopeObject ...objs) {
		var res = new ScopeObject(proto);

		for (var el : objs) {
			res.fields.putAll(el.fields);
		}

		return res;
	}

	public static ScopeObject all(Frame frame, String[] local, String[] capturables, String[] captures) {
		return combine((ObjectValue)Value.global(frame.env), locals(frame, local), capturables(frame, capturables), captures(frame, captures));
	}
}
