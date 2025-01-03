package me.topchetoeu.jscript.runtime.values;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import me.topchetoeu.jscript.common.SyntaxException;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.runtime.EventLoop;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Member.PropertyMember;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.functions.NativeFunction;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.BoolValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.SymbolValue;
import me.topchetoeu.jscript.runtime.values.primitives.VoidValue;
import me.topchetoeu.jscript.runtime.values.primitives.numbers.NumberValue;

public abstract class Value {
	public static enum State {
		NORMAL(true, true, true),
		NON_EXTENDABLE(false, true, true),
		SEALED(false, false, true),
		FROZEN(false, false, false);


		public final boolean extendable;
		public final boolean configurable;
		public final boolean writable;

		private State(boolean extendable, boolean configurable, boolean writable) {
			this.extendable = extendable;
			this.writable = writable;
			this.configurable = configurable;
		}
	}

	public static final Key<FunctionValue> REGEX_CONSTR = new Key<>();

	public static final Key<Integer> MAX_STACK_COUNT = new Key<>();
	public static final Key<Boolean> HIDE_STACK = new Key<>();

	public static final Key<ObjectValue> BOOL_PROTO = new Key<>();
	public static final Key<ObjectValue> NUMBER_PROTO = new Key<>();
	public static final Key<ObjectValue> STRING_PROTO = new Key<>();
	public static final Key<ObjectValue> SYMBOL_PROTO = new Key<>();

	public static final Key<ObjectValue> OBJECT_PROTO = new Key<>();
	public static final Key<ObjectValue> FUNCTION_PROTO = new Key<>();

	public static final Key<ObjectValue> ARRAY_PROTO = new Key<>();
	public static final Key<ObjectValue> BYTE_BUFF_PROTO = new Key<>();

	public static final Key<ObjectValue> ERROR_PROTO = new Key<>();
	public static final Key<ObjectValue> SYNTAX_ERR_PROTO = new Key<>();
	public static final Key<ObjectValue> TYPE_ERR_PROTO = new Key<>();
	public static final Key<ObjectValue> RANGE_ERR_PROTO = new Key<>();

	public static final Key<Value> GLOBAL = new Key<>();
	public static final Key<Map<String, Value>> INTRINSICS = new Key<>();

	public static final VoidValue UNDEFINED = new VoidValue("undefined", "undefined");
	public static final VoidValue NULL = new VoidValue("null", "object");

	public abstract StringValue type();
	public abstract boolean isPrimitive();

	public final boolean isNaN() {
		return this == NumberValue.NAN || this instanceof NumberValue num && Double.isNaN(num.getDouble());
	}

	public Value apply(Environment env, Value self, Value ...args) {
		throw EngineException.ofType("Value is not a function");
	}
	public Value construct(Environment env, Value target, Value ...args) {
		throw EngineException.ofType("Value is not a constructor");
	}

	public final Value constructNoSelf(Environment env, Value ...args) {
		return this.construct(env, this, args);
	}


	public abstract Value toPrimitive(Environment env);
	public abstract NumberValue toNumber(Environment env);
	public abstract String toString(Environment env);
	public abstract boolean toBoolean();

	public final boolean isInstanceOf(Environment env, Value proto) {
		for (var val = getPrototype(env); val != null; val = val.getPrototype(env)) {
			if (val.equals(proto)) return true;
		}

		return false;
	}

	public abstract Member getOwnMember(Environment env, KeyCache key);
	public abstract Set<String> getOwnMembers(Environment env, boolean onlyEnumerable);
	public abstract Set<SymbolValue> getOwnSymbolMembers(Environment env, boolean onlyEnumerable);
	public abstract boolean defineOwnField(Environment env, KeyCache key, Value val, Boolean writable, Boolean enumerable, Boolean configurable);
	public abstract boolean defineOwnProperty(Environment env, KeyCache key, Optional<FunctionValue> get, Optional<FunctionValue> set, Boolean enumerable, Boolean configurable);
	public abstract boolean deleteOwnMember(Environment env, KeyCache key);

	public abstract ObjectValue getPrototype(Environment env);
	public abstract boolean setPrototype(Environment env, ObjectValue val);

	public abstract State getState();

	public abstract void preventExtensions();
	public abstract void seal();
	public abstract void freeze();

	public final Member getOwnMember(Environment env, Value key) {
		return getOwnMember(env, new KeyCache(key));
	}
	public final Member getOwnMember(Environment env, String key) {
		return getOwnMember(env, new KeyCache(key));
	}
	public final Member getOwnMember(Environment env, int key) {
		return getOwnMember(env, new KeyCache(key));
	}
	public final Member getOwnMember(Environment env, double key) {
		return getOwnMember(env, new KeyCache(key));
	}

	public final boolean defineOwnProperty(Environment env, Value key, Optional<FunctionValue> get, Optional<FunctionValue> set, Boolean enumerable, Boolean configurable) {
		return defineOwnProperty(env, new KeyCache(key), get, set, enumerable, configurable);
	}
	public final boolean defineOwnProperty(Environment env, String key, Optional<FunctionValue> get, Optional<FunctionValue> set, Boolean enumerable, Boolean configurable) {
		return defineOwnProperty(env, new KeyCache(key), get, set, enumerable, configurable);
	}
	public final boolean defineOwnProperty(Environment env, int key, Optional<FunctionValue> get, Optional<FunctionValue> set, Boolean enumerable, Boolean configurable) {
		return defineOwnProperty(env, new KeyCache(key), get, set, enumerable, configurable);
	}
	public final boolean defineOwnProperty(Environment env, double key, Optional<FunctionValue> get, Optional<FunctionValue> set, Boolean enumerable, Boolean configurable) {
		return defineOwnProperty(env, new KeyCache(key), get, set, enumerable, configurable);
	}

	public final boolean defineOwnField(Environment env, Value key, Value val, Boolean writable, Boolean enumerable, Boolean configurable) {
		return defineOwnField(env, new KeyCache(key), val, writable, enumerable, configurable);
	}
	public final boolean defineOwnField(Environment env, String key, Value val, Boolean writable, Boolean enumerable, Boolean configurable) {
		return defineOwnField(env, new KeyCache(key), val, writable, enumerable, configurable);
	}
	public final boolean defineOwnField(Environment env, int key, Value val, Boolean writable, Boolean enumerable, Boolean configurable) {
		return defineOwnField(env, new KeyCache(key), val, writable, enumerable, configurable);
	}
	public final boolean defineOwnField(Environment env, double key, Value val, Boolean writable, Boolean enumerable, Boolean configurable) {
		return defineOwnField(env, new KeyCache(key), val, writable, enumerable, configurable);
	}

	public final boolean defineOwnField(Environment env, KeyCache key, Value val) {
		return defineOwnField(env, key, val, true, true, true);
	}
	public final boolean defineOwnField(Environment env, Value key, Value val) {
		return defineOwnField(env, new KeyCache(key), val);
	}
	public final boolean defineOwnField(Environment env, String key, Value val) {
		return defineOwnField(env, new KeyCache(key), val);
	}
	public final boolean defineOwnField(Environment env, int key, Value val) {
		return defineOwnField(env, new KeyCache(key), val);
	}
	public final boolean defineOwnField(Environment env, double key, Value val) {
		return defineOwnField(env, new KeyCache(key), val);
	}

	public final boolean deleteOwnMember(Environment env, Value key) {
		return deleteOwnMember(env, new KeyCache(key));
	}
	public final boolean deleteOwnMember(Environment env, String key) {
		return deleteOwnMember(env, new KeyCache(key));
	}
	public final boolean deleteOwnMember(Environment env, int key) {
		return deleteOwnMember(env, new KeyCache(key));
	}
	public final boolean deleteOwnMember(Environment env, double key) {
		return deleteOwnMember(env, new KeyCache(key));
	}

	public final Value getMemberOrNull(Environment env, KeyCache key) {
		for (Value obj = this; obj != null; obj = obj.getPrototype(env)) {
			var member = obj.getOwnMember(env, key);
			if (member != null) return member.get(env, this);
		}

		return null;
	}
	public final Value getMemberOrNull(Environment env, Value key) {
		return getMemberOrNull(env, new KeyCache(key));
	}
	public final Value getMemberOrNull(Environment env, String key) {
		return getMemberOrNull(env, new KeyCache(key));
	}
	public final Value getMemberOrNull(Environment env, int key) {
		return getMemberOrNull(env, new KeyCache(key));
	}
	public final Value getMemberOrNull(Environment env, double key) {
		return getMemberOrNull(env, new KeyCache(key));
	}

	public final Value getMember(Environment env, KeyCache key) {
		var res = getMemberOrNull(env, key);
		if (res != null) return res;
		else return Value.UNDEFINED;
	}
	public final Value getMember(Environment env, Value key) {
		return getMember(env, new KeyCache(key));
	}
	public final Value getMember(Environment env, String key) {
		return getMember(env, new KeyCache(key));
	}
	public final Value getMember(Environment env, int key) {
		return getMember(env, new KeyCache(key));
	}
	public final Value getMember(Environment env, double key) {
		return getMember(env, new KeyCache(key));
	}

	public final boolean setMember(Environment env, KeyCache key, Value val) {
		for (Value obj = this; obj != null; obj = obj.getPrototype(env)) {
			var member = obj.getOwnMember(env, key);
			if (member != null && (member instanceof PropertyMember || obj == this)) {
				if (member.set(env, val, this)) {
					if (val instanceof FunctionValue && !key.isSymbol()) ((FunctionValue)val).setName(key.toString(env));
					return true;
				}
				else return false;
			}
		}

		if (defineOwnField(env, key, val)) {
			if (val instanceof FunctionValue func) {
				if (key.isSymbol()) func.setName(key.toSymbol().toString());
				else func.setName(key.toString(env));
			}
			return true;
		}
		else return false;
	}
	public final boolean setMember(Environment env, Value key, Value val) {
		return setMember(env, new KeyCache(key), val);
	}
	public final boolean setMember(Environment env, String key, Value val) {
		return setMember(env, new KeyCache(key), val);
	}
	public final boolean setMember(Environment env, int key, Value val) {
		return setMember(env, new KeyCache(key), val);
	}
	public final boolean setMember(Environment env, double key, Value val) {
		return setMember(env, new KeyCache(key), val);
	}

	public final boolean setMemberIfExists(Environment env, KeyCache key, Value val) {
		for (Value obj = this; obj != null; obj = obj.getPrototype(env)) {
			var member = obj.getOwnMember(env, key);
			if (member != null) {
				if (member.set(env, val, obj)) {
					if (!key.isSymbol() && val instanceof FunctionValue) ((FunctionValue)val).setName(key.toString(env));
					return true;
				}
				else return false;
			}
		}

		return false;
	}
	public final boolean setMemberIfExists(Environment env, Value key, Value val) {
		return setMemberIfExists(env, new KeyCache(key), val);
	}
	public final boolean setMemberIfExists(Environment env, String key, Value val) {
		return setMemberIfExists(env, new KeyCache(key), val);
	}
	public final boolean setMemberIfExists(Environment env, int key, Value val) {
		return setMemberIfExists(env, new KeyCache(key), val);
	}
	public final boolean setMemberIfExists(Environment env, double key, Value val) {
		return setMemberIfExists(env, new KeyCache(key), val);
	}

	public final boolean hasMember(Environment env, KeyCache key, boolean own) {
		for (Value obj = this; obj != null; obj = obj.getPrototype(env)) {
			if (obj.getOwnMember(env, key) != null) return true;
			if (own) return false;
		}

		return false;
	}
	public final boolean hasMember(Environment env, Value key, boolean own) {
		return hasMember(env, new KeyCache(key), own);
	}
	public final boolean hasMember(Environment env, String key, boolean own) {
		return hasMember(env, new KeyCache(key), own);
	}
	public final boolean hasMember(Environment env, int key, boolean own) {
		return hasMember(env, new KeyCache(key), own);
	}
	public final boolean hasMember(Environment env, double key, boolean own) {
		return hasMember(env, new KeyCache(key), own);
	}

	public final boolean deleteMember(Environment env, KeyCache key) {
		if (!hasMember(env, key, true)) return true;
		return deleteOwnMember(env, key);
	}
	public final boolean deleteMember(Environment env, Value key) {
		return deleteMember(env, new KeyCache(key));
	}
	public final boolean deleteMember(Environment env, String key) {
		return deleteMember(env, new KeyCache(key));
	}
	public final boolean deleteMember(Environment env, int key) {
		return deleteMember(env, new KeyCache(key));
	}
	public final boolean deleteMember(Environment env, double key) {
		return deleteMember(env, new KeyCache(key));
	}

	public final Set<String> getMembers(Environment env, boolean own, boolean onlyEnumerable) {
		var res = new LinkedHashSet<String>();
		var protos = new ArrayList<Value>();

		for (var proto = this; proto != null; proto = proto.getPrototype(env)) {
			protos.add(proto);
			if (own) break;
		}

		Collections.reverse(protos);

		for (var proto : protos) {
			res.addAll(proto.getOwnMembers(env, onlyEnumerable));
		}

		return res;
	}
	public final Set<SymbolValue> getSymbolMembers(Environment env, boolean own, boolean onlyEnumerable) {
		var res = new LinkedHashSet<SymbolValue>();
		var protos = new ArrayList<Value>();

		for (var proto = this; proto != null; proto = proto.getPrototype(env)) {
			protos.add(proto);
			if (own) break;
		}

		Collections.reverse(protos);

		for (var proto : protos) {
			res.addAll(proto.getOwnSymbolMembers(env, onlyEnumerable));
		}

		return res;
	}

	public final Value getMemberPath(Environment env, String ...path) {
		var res = this;
		for (var key : path) res = res.getMember(env, key);
		return res;
	}
	public final Value getMemberPath(Environment env, Value ...path) {
		var res = this;
		for (var key : path) res = res.getMember(env, key);
		return res;
	}
	public final ObjectValue getMemberDescriptor(Environment env, Value key) {
		var member = getOwnMember(env, new KeyCache(key));

		if (member != null) return member.descriptor(env, this);
		else return null;
	}

	public Iterable<Object> toIterable(Environment env) {
		return () -> {
			if (!(this instanceof FunctionValue)) return Collections.emptyIterator();
			var func = (FunctionValue)this;

			return new Iterator<Object>() {
				private Object value = null;
				public boolean consumed = true;
				private FunctionValue supplier = func;

				private void loadNext() {
					if (supplier == null) value = null;
					else if (consumed) {
						var curr = supplier.apply(env, Value.UNDEFINED);

						if (curr == null) { supplier = null; value = null; }
						if (curr.getMember(env, StringValue.of("done")).toBoolean()) { supplier = null; value = null; }
						else {
							this.value = curr.getMember(env, StringValue.of("value"));
							consumed = false;
						}
					}
				}

				@Override public boolean hasNext() {
					loadNext();
					return supplier != null;
				}
				@Override public Object next() {
					loadNext();
					var res = value;
					value = null;
					consumed = true;
					return res;
				}
			};
		};
	}

	public void callWith(Environment env, Iterable<? extends Value> it) {
		for (var el : it) {
			this.apply(env, Value.UNDEFINED, el);
		}
	}
	public void callWithAsync(Environment env, Iterable<? extends Value> it, boolean async) {
		for (var el : it) {
			env.get(EventLoop.KEY).pushMsg(() -> this.apply(env, Value.UNDEFINED, el), true);
		}
	}

	/** @internal */
	public List<String> toReadableLines(Environment env, HashSet<ObjectValue> passed) {
		return Arrays.asList(toString(env));
	}

	public final String toReadable(Environment ext) {
		return String.join("\n", toReadableLines(ext, new HashSet<>()));
	}

	public static final Value global(Environment env) {
		return env.initFrom(GLOBAL, () -> new ObjectValue());
	}
	public static final Map<String, Value> intrinsics(Environment env) {
		return env.initFrom(INTRINSICS, () -> new HashMap<>());
	}

	public static FunctionValue fromIterator(Environment ext, Iterable<? extends Value> iterable) {
		var it = iterable.iterator();

		return new NativeFunction("", args -> {
			var obj = new ObjectValue();

			if (!it.hasNext()) obj.defineOwnField(args.env, "done", BoolValue.TRUE);
			else obj.defineOwnField(args.env, "value", it.next());

			return obj;
		});
	}

	public static final boolean lessOrEqual(Environment env, Value a, Value b) {
		a = a.toPrimitive(env);
		b = b.toPrimitive(env);

		if (a instanceof StringValue aStr && b instanceof StringValue bStr) {
			return aStr.value.compareTo(bStr.value) <= 0;
		}
		else {
			var na = a.toNumber(env);
			var nb = b.toNumber(env);

			if (na.isLong() && nb.isLong()) return na.getLong() <= nb.getLong();
			else return na.getDouble() <= nb.getDouble();
		}
	}
	public static final boolean greaterOrEqual(Environment env, Value a, Value b) {
		a = a.toPrimitive(env);
		b = b.toPrimitive(env);

		if (a instanceof StringValue aStr && b instanceof StringValue bStr) {
			return aStr.value.compareTo(bStr.value) >= 0;
		}
		else {
			var na = a.toNumber(env);
			var nb = b.toNumber(env);

			if (na.isLong() && nb.isLong()) return na.getLong() >= nb.getLong();
			else return na.getDouble() >= nb.getDouble();
		}
	}
	public static final boolean less(Environment env, Value a, Value b) {
		a = a.toPrimitive(env);
		b = b.toPrimitive(env);

		if (a instanceof StringValue aStr && b instanceof StringValue bStr) {
			return aStr.value.compareTo(bStr.value) < 0;
		}
		else {
			var na = a.toNumber(env);
			var nb = b.toNumber(env);

			if (na.isLong() && nb.isLong()) return na.getLong() < nb.getLong();
			else return na.getDouble() < nb.getDouble();
		}
	}
	public static final boolean greater(Environment env, Value a, Value b) {
		a = a.toPrimitive(env);
		b = b.toPrimitive(env);

		if (a instanceof StringValue aStr && b instanceof StringValue bStr) {
			return aStr.value.compareTo(bStr.value) > 0;
		}
		else {
			var na = a.toNumber(env);
			var nb = b.toNumber(env);

			if (na.isLong() && nb.isLong()) return na.getLong() > nb.getLong();
			else return na.getDouble() > nb.getDouble();
		}
	}

	public static final Value add(Environment env, Value a, Value b) {
		a = a.toPrimitive(env);
		b = b.toPrimitive(env);

		if (a instanceof StringValue || b instanceof StringValue) {
			return StringValue.of(a.toString(env) + b.toString(env));
		}
		else {
			var na = a.toNumber(env);
			var nb = b.toNumber(env);

			if (na.isInt() && nb.isInt()) return NumberValue.of(na.getInt() + nb.getInt());
			else return NumberValue.of(na.getDouble() + nb.getDouble());
		}
	}

	public static final NumberValue subtract(Environment env, Value a, Value b) {
		var na = a.toNumber(env);
		var nb = b.toNumber(env);

		if (na.isInt() && nb.isInt()) return NumberValue.of(na.getInt() - nb.getInt());
		else return NumberValue.of(na.getDouble() - nb.getDouble());
	}
	public static final NumberValue multiply(Environment env, Value a, Value b) {
		var na = a.toNumber(env);
		var nb = b.toNumber(env);

		if (na.isInt() && nb.isInt()) return NumberValue.of(na.getInt() * nb.getInt());
		else return NumberValue.of(na.getDouble() * nb.getDouble());
	}
	public static final NumberValue divide(Environment env, Value a, Value b) {
		var na = a.toNumber(env);
		var nb = b.toNumber(env);

		if (na.isInt() && nb.isInt()) {
			var ia = na.getInt();
			var ib = nb.getInt();

			if (ib == 0) {
				if (ia == 0) return NumberValue.NAN;
				else if (ia > 0) return NumberValue.of(Double.POSITIVE_INFINITY);
				else return NumberValue.of(Double.NEGATIVE_INFINITY);
			}
			else if (ia % ib != 0) return NumberValue.of((double)ia / ib);
			else return NumberValue.of(ia / ib);
		}
		else return NumberValue.of(na.getDouble() / nb.getDouble());
	}
	public static final NumberValue modulo(Environment env, Value a, Value b) {
		var na = a.toNumber(env);
		var nb = b.toNumber(env);

		if (na.isInt() && nb.isInt()) {
			var ia = na.getInt();
			var ib = nb.getInt();

			if (ib == 0) return NumberValue.NAN;
			else return NumberValue.of(ia % ib);
		}
		else return NumberValue.of(na.getDouble() % nb.getDouble());
	}
	public static final NumberValue negative(Environment env, Value a) {
		var na = a.toNumber(env);

		if (na.isInt()) return NumberValue.of(-na.getInt());
		else return NumberValue.of(-na.getDouble());
	}

	public static final NumberValue and(Environment env, Value a, Value b) {
		return NumberValue.of(a.toNumber(env).getInt() & b.toNumber(env).getInt());
	}
	public static final NumberValue or(Environment env, Value a, Value b) {
		return NumberValue.of(a.toNumber(env).getInt() | b.toNumber(env).getInt());
	}
	public static final NumberValue xor(Environment env, Value a, Value b) {
		return NumberValue.of(a.toNumber(env).getInt() ^ b.toNumber(env).getInt());
	}
	public static final NumberValue bitwiseNot(Environment env, Value a) {
		return NumberValue.of(~a.toNumber(env).getInt());
	}

	public static final NumberValue shiftLeft(Environment env, Value a, Value b) {
		return NumberValue.of(a.toNumber(env).getInt() << b.toNumber(env).getInt());
	}
	public static final NumberValue shiftRight(Environment env, Value a, Value b) {
		return NumberValue.of(a.toNumber(env).getInt() >> b.toNumber(env).getInt());
	}
	public static final NumberValue unsignedShiftRight(Environment env, Value a, Value b) {
		long _a = a.toNumber(env).getLong() & 0xFFFFFFFF;
		long _b = b.toNumber(env).getLong() & 0xFFFFFFFF;

		if (_a < 0) _a += 0x100000000l;
		if (_b < 0) _b += 0x100000000l;

		return NumberValue.of(_a >>> _b);
	}

	public static final boolean looseEqual(Environment env, Value a, Value b) {
		// In loose equality, null is equivalent to undefined
		if (a instanceof VoidValue || b instanceof VoidValue) return a instanceof VoidValue && b instanceof VoidValue;

		// If both are objects, just compare their references
		if (!a.isPrimitive() && !b.isPrimitive()) return a.equals(b);

		// Convert values to primitives
		a = a.toPrimitive(env);
		b = b.toPrimitive(env);

		// Compare symbols by reference
		if (a instanceof SymbolValue || b instanceof SymbolValue) return a.equals(b);
		// Compare booleans as numbers
		if (a instanceof BoolValue || b instanceof BoolValue) return a.toNumber(env).equals(b.toNumber(env));
		// Comparse numbers as numbers
		if (a instanceof NumberValue || b instanceof NumberValue) return a.toNumber(env).equals(b.toNumber(env));

		// Default to strings
		return a.toString(env).equals(b.toString(env));
	}

	public static final String errorToReadable(Environment env, RuntimeException err, String prefix) {
		prefix = prefix == null ? "Uncaught" : "Uncaught " + prefix;
		if (err instanceof EngineException ee) {
			if (env == null) env = ee.env;

			try {
				return prefix + " " + ee.toString(env);
			}
			catch (EngineException ex) {
				return prefix + " " + ee.value.toReadable(env);
			}
		}
		else if (err instanceof SyntaxException syntax) {
			var newErr = EngineException.ofSyntax(syntax.msg);
			newErr.add(null, syntax.loc.filename() + "", syntax.loc);
			return errorToReadable(env, newErr, prefix);
		}
		else if (err.getCause() instanceof InterruptedException) return "";
		else {
			var str = new ByteArrayOutputStream();
			err.printStackTrace(new PrintStream(str));

			return prefix + " internal error " + str.toString();
		}
	}
}
