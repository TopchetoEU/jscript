package me.topchetoeu.jscript.runtime.values;

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

        @Override public boolean redefine(Environment env, Member newMember, Value self) {
            // If the given member isn't a property, we can't redefine
            if (!(newMember instanceof PropertyMember prop)) return false;

            if (configurable()) {
                // We will overlay the getters and setters of the new member
                enumerable = prop.enumerable;
                configurable = prop.configurable;

                if (prop.getter != null) getter = prop.getter;
                if (prop.setter != null) setter = prop.setter;

                return true;
            }
            else {
                // We will pretend that a redefinition has occurred if the two members match exactly
                if (prop.configurable() != configurable()) return false;
                if (prop.enumerable != enumerable) return false;
                if (prop.getter != getter || prop.setter != setter) return false;

                return true;
            }
        }

        @Override public ObjectValue descriptor(Environment env, Value self) {
            var res = new ObjectValue();

            // Don't touch the ordering, as it's emulating V8

            if (getter == null) res.defineOwnMember(env, "getter", Value.UNDEFINED);
            else res.defineOwnMember(env, "getter", getter);

            if (setter == null) res.defineOwnMember(env, "setter", Value.UNDEFINED);
            else res.defineOwnMember(env, "setter", setter);

            res.defineOwnMember(env, "enumerable", BoolValue.of(enumerable));
            res.defineOwnMember(env, "configurable", BoolValue.of(configurable));
            return res;
        }

        public PropertyMember(Value self, FunctionValue getter, FunctionValue setter, boolean configurable, boolean enumerable) {
            this.self = self;
            this.getter = getter;
            this.setter = setter;
            this.configurable = configurable;
            this.enumerable = enumerable;
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
            public SimpleFieldMember(Value self, Value value, boolean configurable, boolean enumerable, boolean writable) {
                super(self, configurable, enumerable, writable);
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

        @Override public final boolean redefine(Environment env, Member newMember, Value self) {
            // If the given member isn't a field, we can't redefine
            if (!(newMember instanceof FieldMember field)) return false;

            if (configurable()) {
                configurable = field.configurable;
                enumerable = field.enumerable;
                writable = field.enumerable;

                // We will try to set a new value. However, the underlying field might be immutably readonly
                // In such case, we will silently fail, since this is not covered by the specification
                if (!set(env, field.get(env, self), self)) writable = false;
                return true;
            }
            else {
                // New field settings must be an exact match
                if (configurable() != field.configurable()) return false;
                if (enumerable() != field.enumerable()) return false;

                if (!writable()) {
                    // If the field isn't writable, the redefinition should be an exact match
                    if (field.writable()) return false;
                    if (field.get(env, self).equals(this.get(env, self))) return false;

                    return true;
                }
                else {
                    // Writable non-configurable fields may be made readonly or their values may be changed
                    writable = field.writable;

                    if (!set(env, field.get(env, self), self)) writable = false;
                    return true;
                }
            }
        }

        @Override public ObjectValue descriptor(Environment env, Value self) {
            var res = new ObjectValue();
            res.defineOwnMember(env, "value", get(env, self));
            res.defineOwnMember(env, "writable", BoolValue.of(writable));
            res.defineOwnMember(env, "enumerable", BoolValue.of(enumerable));
            res.defineOwnMember(env, "configurable", BoolValue.of(configurable));
            return res;
        }

        public FieldMember(Value self, boolean configurable, boolean enumerable, boolean writable) {
            this.self = self;
            this.configurable = configurable;
            this.enumerable = enumerable;
            this.writable = writable;
        }

        public static FieldMember of(Value self, Value value) {
            return new SimpleFieldMember(self, value, true, true, true);
        }
        public static FieldMember of(Value self, Value value, boolean writable) {
            return new SimpleFieldMember(self, value, true, true, writable);
        }
        public static FieldMember of(Value self, Value value, boolean configurable, boolean enumerable, boolean writable) {
            return new SimpleFieldMember(self, value, configurable, enumerable, writable);
        }
    }

    public boolean configurable();
    public boolean enumerable();
    public boolean redefine(Environment env, Member newMember, Value self);
    public ObjectValue descriptor(Environment env, Value self);

    public Value get(Environment env, Value self);
    public boolean set(Environment env, Value val, Value self);
}