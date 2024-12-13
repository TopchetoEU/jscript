package me.topchetoeu.jscript.runtime.values;

import java.util.Optional;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.BoolValue;

public interface Member {
	public static final class PropertyMember implements Member {
		public final Value self;
		public FunctionValue getter;
		public FunctionValue setter;
		public boolean configurable;
		public boolean enumerable;

		@Override public Value get(Environment env, Value self) {
			if (getter != null) return getter.apply(env, self);
			else return Value.UNDEFINED;
		}
		@Override public boolean set(Environment env, Value val, Value self) {
			if (setter == null) return false;
			setter.apply(env, self, val);
			return true;
		}

		@Override public boolean configurable() { return configurable && self.getState().configurable; }
		@Override public boolean enumerable() { return enumerable; }

		public boolean reconfigure(
			Environment env, Value self,
			Optional<FunctionValue> get, Optional<FunctionValue> set,
			Boolean enumerable, Boolean configurable
		) {
			if (this.configurable) {
				// We will overlay the getters and setters of the new member
				if (enumerable != null) this.enumerable = enumerable;
				if (configurable != null) this.configurable = configurable;

				if (get != null) this.getter = get.orElse(null);
				if (set != null) this.setter = set.orElse(null);

				return true;
			}
			else {
				// We will pretend that a redefinition has occurred if the two members match exactly
				if (configurable != null && this.configurable != configurable) return false;
				if (enumerable != null && this.enumerable != enumerable) return false;
				if (get != null && get.orElse(null) != getter) return false;
				if (set != null && set.orElse(null) != setter) return false;

				return true;
			}
		}

		@Override public ObjectValue descriptor(Environment env, Value self) {
			var res = new ObjectValue();

			// Don't touch the ordering, as it's emulating V8

			if (getter == null) res.defineOwnField(env, "getter", Value.UNDEFINED);
			else res.defineOwnField(env, "getter", getter);

			if (setter == null) res.defineOwnField(env, "setter", Value.UNDEFINED);
			else res.defineOwnField(env, "setter", setter);

			res.defineOwnField(env, "enumerable", BoolValue.of(enumerable));
			res.defineOwnField(env, "configurable", BoolValue.of(configurable));
			return res;
		}

		public PropertyMember(Value self, FunctionValue getter, FunctionValue setter, boolean configurable, boolean enumerable) {
			this.self = self;
			this.getter = getter;
			this.setter = setter;
			this.configurable = configurable;
			this.enumerable = enumerable;
		}
		public PropertyMember(Value self, Optional<FunctionValue> getter, Optional<FunctionValue> setter, Boolean configurable, Boolean enumerable) {
			this.self = self;
			this.getter = getter == null ? null : getter.orElse(null);
			this.setter = setter == null ? null : setter.orElse(null);
			this.configurable = configurable == null ? false : configurable;
			this.enumerable = enumerable == null ? false : enumerable;
		}
	}

	public static abstract class FieldMember implements Member {
		private static class SimpleFieldMember extends FieldMember {
			public Value value;

			@Override public Value get(Environment env, Value self) { return value; }
			@Override public boolean set(Environment env, Value val, Value self) {
				if (!writable) return false;
				value = val;
				return true;
			}
			public SimpleFieldMember(Value self, Value value, Boolean configurable, Boolean enumerable, Boolean writable) {
				super(self, configurable, enumerable, writable);
				if (value == null) value = Value.UNDEFINED;

				this.value = value;
			}
		}

		public final Value self;
		public boolean configurable;
		public boolean enumerable;
		public boolean writable;

		@Override public final boolean configurable() { return configurable && self.getState().configurable; }
		@Override public final boolean enumerable() { return enumerable; }
		public final boolean writable() { return writable && self.getState().writable; }

		public final boolean reconfigure(
			Environment env, Value self, Value val,
			Boolean writable, Boolean enumerable, Boolean configurable
		) {
			if (this.configurable) {
				if (writable != null) this.writable = writable;
				if (enumerable != null) this.enumerable = enumerable;
				if (configurable != null) this.configurable = configurable;
				if (val != null) {
					// We will try to set a new value. However, the underlying field might be immutably readonly
					// In such case, we will silently fail, since this is not covered by the specification
					if (!set(env, val, self)) writable = false;
				}

				return true;
			}
			else {
				// New field settings must be an exact match
				if (configurable != null && this.configurable != configurable) return false;
				if (enumerable != null && this.enumerable != enumerable) return false;

				if (this.writable) {
					// If the field isn't writable, the redefinition should be an exact match
					if (writable != null && writable) return false;
					if (val != null && val.equals(this.get(env, self))) return false;

					return true;
				}
				else {
					// Writable non-configurable fields may be made readonly or their values may be changed
					if (writable != null) this.writable = writable;

					if (!set(env, val, self)) writable = false;
					return true;
				}
			}
		}

		@Override public ObjectValue descriptor(Environment env, Value self) {
			var res = new ObjectValue();
			res.defineOwnField(env, "value", get(env, self));
			res.defineOwnField(env, "writable", BoolValue.of(writable));
			res.defineOwnField(env, "enumerable", BoolValue.of(enumerable));
			res.defineOwnField(env, "configurable", BoolValue.of(configurable));
			return res;
		}

		public FieldMember(Value self, Boolean configurable, Boolean enumerable, Boolean writable) {
			if (writable == null) writable = false;
			if (enumerable == null) enumerable = false;
			if (configurable == null) configurable = false;

			this.self = self;
			this.configurable = configurable;
			this.enumerable = enumerable;
			this.writable = writable;
		}

		public static FieldMember of(Value self, Value value) {
			return new SimpleFieldMember(self, value, true, true, true);
		}
		public static FieldMember of(Value self, Value value, Boolean writable) {
			return new SimpleFieldMember(self, value, true, true, writable);
		}
		public static FieldMember of(Value self, Value value, Boolean configurable, Boolean enumerable, Boolean writable) {
			return new SimpleFieldMember(self, value, configurable, enumerable, writable);
		}
	}

	public boolean configurable();
	public boolean enumerable();
	public ObjectValue descriptor(Environment env, Value self);

	public Value get(Environment env, Value self);
	public boolean set(Environment env, Value val, Value self);
}