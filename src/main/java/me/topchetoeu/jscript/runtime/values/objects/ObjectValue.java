package me.topchetoeu.jscript.runtime.values.objects;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.Member.PropertyMember;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.SymbolValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

public class ObjectValue extends Value {
	public static interface PrototypeProvider {
		public ObjectValue get(Environment env);
	}

	public static class Property { 
		public final FunctionValue getter;
		public final FunctionValue setter;

		public Property(FunctionValue getter, FunctionValue setter) {
			this.getter = getter;
			this.setter = setter;
		}
	}

	protected PrototypeProvider prototype;

	private HashMap<String, FieldMember> fields = new HashMap<>();
	private HashMap<SymbolValue, FieldMember> symbolFields = new HashMap<>();
	private HashMap<String, PropertyMember> properties = new HashMap<>();
	private HashMap<SymbolValue, PropertyMember> symbolProperties = new HashMap<>();

	private LinkedHashMap<String, Boolean> keys = new LinkedHashMap<>();
	private LinkedHashMap<SymbolValue, Boolean> symbols = new LinkedHashMap<>();

	@Override public boolean isPrimitive() { return false; }
	@Override public Value toPrimitive(Environment env) {
		if (env != null) {
			var valueOf = getMember(env, "valueOf");

			if (valueOf instanceof FunctionValue) {
				var res = valueOf.apply(env, this);
				if (res.isPrimitive()) return res;
			}

			var toString = getMember(env, "toString");
			if (toString instanceof FunctionValue) {
				var res = toString.apply(env, this);
				if (res.isPrimitive()) return res;
			}
		}

		throw EngineException.ofType("Value couldn't be converted to a primitive.");
	}
	@Override public String toString(Environment env) { return toPrimitive(env).toString(env); }
	@Override public boolean toBoolean() { return true; }
	@Override public NumberValue toNumber(Environment env) { return toPrimitive(env).toNumber(env);  }
	@Override public StringValue type() { return StringValue.of("object"); }

	private State state = State.NORMAL;

	@Override public State getState() { return state; }

	public final void preventExtensions() {
		if (state == State.NORMAL) state = State.NON_EXTENDABLE;
	}
	public final void seal() {
		if (state == State.NORMAL || state == State.NON_EXTENDABLE) state = State.SEALED;
	}
	@Override public final void freeze() { state = State.FROZEN; }

	@Override public Member getOwnMember(Environment env, KeyCache key) {
		if (key.isSymbol()) {
			if (!symbols.containsKey(key.toSymbol())) return null;
			
			if (symbols.get(key.toSymbol())) return symbolProperties.get(key.toSymbol());
			else return symbolFields.get(key.toSymbol());
		}
		else if (keys.containsKey(key.toString(env))) {
			if (keys.get(key.toString(env))) return properties.get(key.toString(env));
			else return fields.get(key.toString(env));
		}
		else return null;
	}
	@Override public boolean defineOwnField(
		Environment env, KeyCache key, Value val,
		Boolean writable, Boolean enumerable, Boolean configurable
	) {
		if (key.isSymbol()) {

			if (symbols.containsKey(key.toSymbol())) {
				if (symbols.get(key.toSymbol())) {
					var prop = symbolProperties.get(key.toSymbol());
					if (!prop.configurable) return false;

					symbolProperties.remove(key.toSymbol());
				}
				else return symbolFields.get(key.toSymbol()).reconfigure(env, this, val, writable, enumerable, configurable);
			}

			symbols.put(key.toSymbol(), false);
			symbolFields.put(key.toSymbol(), FieldMember.of(this, val, writable, enumerable, configurable));
			return true;
		}
		else if (keys.containsKey(key.toString(env))) {
			if (keys.get(key.toString(env))) {
				var prop = properties.get(key.toString(env));
				if (!prop.configurable) return false;

				properties.remove(key.toString(env));
			}
			else return fields.get(key.toString(env)).reconfigure(env, this, val, writable, enumerable, configurable);
		}

		keys.put(key.toString(env), false);
		fields.put(key.toString(env), FieldMember.of(this, val, writable, enumerable, configurable));
		return true;
	}
	@Override public boolean defineOwnProperty(
		Environment env, KeyCache key,
		Optional<FunctionValue> get, Optional<FunctionValue> set,
		Boolean enumerable, Boolean configurable
	) {
		if (key.isSymbol()) {
			if (symbols.containsKey(key.toSymbol())) {
				if (!symbols.get(key.toSymbol())) {
					var field = symbolFields.get(key.toSymbol());
					if (!field.configurable) return false;

					symbolFields.remove(key.toSymbol());
				}
				else return symbolProperties.get(key.toSymbol()).reconfigure(env, this, get, set, enumerable, configurable);
			}

			symbols.put(key.toSymbol(), true);
			symbolProperties.put(key.toSymbol(), new PropertyMember(this, get, set, enumerable, configurable));
			return true;
		}
		else if (keys.containsKey(key.toString(env))) {
			if (!keys.get(key.toString(env))) {
				var field = fields.get(key.toString(env));
				if (!field.configurable) return false;

				fields.remove(key.toString(env));
			}
			else return properties.get(key.toString(env)).reconfigure(env, this, get, set, enumerable, configurable);
		}

		keys.put(key.toString(env), true);
		properties.put(key.toString(env), new PropertyMember(this, get, set, enumerable, configurable));
		return true;
	}
	@Override public boolean deleteOwnMember(Environment env, KeyCache key) {
		if (!getState().extendable) return false;

		if (key.isSymbol()) {
			if (!symbols.containsKey(key.toSymbol())) return true;
			
			if (symbols.get(key.toSymbol())) {
				if (!symbolProperties.get(key.toSymbol()).configurable) return false;
				symbolProperties.remove(key.toSymbol());
				symbols.remove(key.toSymbol());
				return true;
			}
			else {
				if (!symbolFields.get(key.toSymbol()).configurable) return false;
				symbolFields.remove(key.toSymbol());
				keys.remove(key.toString(env));
				return true;
			}
		}
		else if (keys.containsKey(key.toString(env))) {
			if (keys.get(key.toString(env))) {
				if (!properties.get(key.toString(env)).configurable) return false;
				properties.remove(key.toString(env));
				symbols.remove(key.toSymbol());
				return true;
			}
			else {
				if (!fields.get(key.toString(env)).configurable) return false;
				fields.remove(key.toString(env));
				keys.remove(key.toString(env));
				return true;
			}
		}
		else return true;
	}

	@Override public Set<String> getOwnMembers(Environment env, boolean onlyEnumerable) {
		if (onlyEnumerable) {
			var res = new LinkedHashSet<String>();

			for (var el : keys.entrySet()) {
				if (el.getValue()) {
					if (properties.get(el.getKey()).enumerable) res.add(el.getKey());
				}
				else {
					if (fields.get(el.getKey()).enumerable) res.add(el.getKey());
				}
			}

			return res;
		}
		else return keys.keySet();
	}
	@Override public Set<SymbolValue> getOwnSymbolMembers(Environment env, boolean onlyEnumerable) {
		if (onlyEnumerable) {
			var res = new LinkedHashSet<SymbolValue>();

			for (var el : symbols.entrySet()) {
				if (el.getValue()) {
					if (symbolProperties.get(el.getKey()).enumerable) res.add(el.getKey());
				}
				else {
					if (symbolFields.get(el.getKey()).enumerable) res.add(el.getKey());
				}
			}

			return res;
		}
		else  return symbols.keySet();
	}

	@Override public ObjectValue getPrototype(Environment env) {
		if (prototype == null || env == null) return null;
		else return prototype.get(env);
	}
	@Override public final boolean setPrototype(Environment env, ObjectValue val) {
		return setPrototype(_env -> val);
	}

	private final LinkedList<String> memberToReadable(Environment env, String key, Member member, HashSet<ObjectValue> passed) {
		if (member instanceof PropertyMember prop) {
			if (prop.getter == null && prop.setter == null) return new LinkedList<>(Arrays.asList(key + ": [No accessors]"));
			else if (prop.getter == null) return new LinkedList<>(Arrays.asList(key + ": [Setter]"));
			else if (prop.setter == null) return new LinkedList<>(Arrays.asList(key + ": [Getter]"));
			else return new LinkedList<>(Arrays.asList(key + ": [Getter/Setter]"));
		}
		else {
			var res = new LinkedList<String>();
			var first = true;

			for (var line : member.get(env, this).toReadableLines(env, passed)) {
				if (first) res.add(key + ": " + line);
				else res.add(line);
				first = false;
			}

			return res;
		}
	}

	public List<String> toReadableLines(Environment env, HashSet<ObjectValue> passed, HashSet<String> ignoredKeys) {
		passed.add(this);

		var stringified = new LinkedList<LinkedList<String>>();

		for (var entry : getOwnSymbolMembers(env, true)) {
			var member = getOwnMember(env, entry);
			stringified.add(memberToReadable(env, "[" + entry.value + "]", member, passed));
		}
		for (var entry : getOwnMembers(env, true)) {
			if (ignoredKeys.contains(entry)) continue;

			var member = getOwnMember(env, entry);
			stringified.add(memberToReadable(env, entry, member, passed));
		}

		passed.remove(this);

		if (stringified.size() == 0) return Arrays.asList("{}");
		var concat = new StringBuilder();
		for (var entry : stringified) {
			// We make a one-liner only when all members are one-liners
			if (entry.size() != 1) {
				concat = null;
				break;
			}

			if (concat.length() != 0) concat.append(", ");
			concat.append(entry.get(0));
		}

		// We don't want too long one-liners
		if (concat != null && concat.length() < 80) return Arrays.asList("{ " + concat.toString() + " }");

		var res = new LinkedList<String>();

		res.add("{");

		for (var entry : stringified) {
			for (var line : entry) {
				res.add("    " + line);
			}

			res.set(res.size() - 1, res.getLast() + ",");
		}
		res.set(res.size() - 1, res.getLast().substring(0, res.getLast().length() - 1));
		res.add("}");

		return res;
	}
	@Override public List<String> toReadableLines(Environment env, HashSet<ObjectValue> passed) {
		return toReadableLines(env, passed, new HashSet<>());
	}

	public final boolean setPrototype(PrototypeProvider val) {
		if (!getState().extendable) return false;
		prototype = val;
		return true;
	}
	public final boolean setPrototype(Key<ObjectValue> key) {
		if (!getState().extendable) return false;
		prototype = env -> env.get(key);
		return true;
	}
}
