package me.topchetoeu.jscript.runtime.values;

import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.BoolValue;
import me.topchetoeu.jscript.runtime.values.primitives.VoidValue;

public interface Member {
    public static final class PropertyMember implements Member {
        public final FunctionValue getter;
        public final FunctionValue setter;
        public final boolean configurable;
        public final boolean enumerable;

        @Override public Value get(Environment env, Value self) {
            if (getter != null) return getter.call(env, self);
            else return VoidValue.UNDEFINED;
        }
        @Override public boolean set(Environment env, Value val, Value self) {
            if (setter == null) return false;
            setter.call(env, self, val);
            return true;
        }

        @Override public boolean configurable() { return configurable; }
        @Override public boolean enumerable() { return enumerable; }

        @Override public boolean configure(Environment env, Member newMember, Value self) {
            if (!(newMember instanceof PropertyMember)) return false;
            var prop = (PropertyMember)newMember;

            if (prop.configurable != configurable) return false;
            if (prop.enumerable != enumerable) return false;

            if (prop.getter == getter) return true;
            if (prop.setter == setter) return true;
            return false;
        }

        @Override public ObjectValue descriptor(Environment env, Value self) {
            var res = new ObjectValue();

            if (getter == null) res.defineOwnMember(env, "getter", FieldMember.of(VoidValue.UNDEFINED));
            else res.defineOwnMember(env, "getter", FieldMember.of(getter));

            if (setter == null) res.defineOwnMember(env, "setter", FieldMember.of(VoidValue.UNDEFINED));
            else res.defineOwnMember(env, "setter", FieldMember.of(setter));

            res.defineOwnMember(env, "enumerable", FieldMember.of(BoolValue.of(enumerable)));
            res.defineOwnMember(env, "configurable", FieldMember.of(BoolValue.of(configurable)));
            return res;
        }

        public PropertyMember(FunctionValue getter, FunctionValue setter, boolean configurable, boolean enumerable) {
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
            public SimpleFieldMember(Value value, boolean configurable, boolean enumerable, boolean writable) {
                super(configurable, enumerable, writable);
                this.value = value;
            }
        }

        public boolean configurable;
        public boolean enumerable;
        public boolean writable;

        @Override public final boolean configurable() { return configurable; }
        @Override public final boolean enumerable() { return enumerable; }
        @Override public final boolean configure(Environment env, Member newMember, Value self) {
            if (!(newMember instanceof FieldMember)) return false;
            var field = (FieldMember)newMember;

            if (field.configurable != configurable) return false;
            if (field.enumerable != enumerable) return false;
            if (!writable) return field.get(env, self).strictEquals(env, get(env, self));

            set(env, field.get(env, self), self);
            writable = field.writable;
            return true;
        }

        @Override public ObjectValue descriptor(Environment env, Value self) {
            var res = new ObjectValue();
            res.defineOwnMember(env, "value", FieldMember.of(get(env, self)));
            res.defineOwnMember(env, "writable", FieldMember.of(BoolValue.of(writable)));
            res.defineOwnMember(env, "enumerable", FieldMember.of(BoolValue.of(enumerable)));
            res.defineOwnMember(env, "configurable", FieldMember.of(BoolValue.of(configurable)));
            return res;
        }

        public FieldMember(boolean configurable, boolean enumerable, boolean writable) {
            this.configurable = configurable;
            this.enumerable = enumerable;
            this.writable = writable;
        }

        public static FieldMember of(Value value) {
            return new SimpleFieldMember(value, true, true, true);
        }
        public static FieldMember of(Value value, boolean writable) {
            return new SimpleFieldMember(value, true, true, writable);
        }
        public static FieldMember of(Value value, boolean configurable, boolean enumerable, boolean writable) {
            return new SimpleFieldMember(value, configurable, enumerable, writable);
        }
    }

    public boolean configurable();
    public boolean enumerable();
    public boolean configure(Environment env, Member newMember, Value self);
    public ObjectValue descriptor(Environment env, Value self);

    public Value get(Environment env, Value self);
    public boolean set(Environment env, Value val, Value self);
}