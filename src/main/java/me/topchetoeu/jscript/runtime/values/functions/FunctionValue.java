package me.topchetoeu.jscript.runtime.values.functions;

import me.topchetoeu.jscript.common.environment.Environment;
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

    public boolean enableCall = true;
    public boolean enableNew = true;

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

    protected abstract Value onCall(Environment ext, boolean isNew, String name, Value thisArg, Value ...args);

    @Override public String toString() { return String.format("function %s(...)", name); }
    @Override public Value call(Environment ext, boolean isNew, String name, Value thisArg, Value ...args) {
        if (isNew && !enableNew) super.call(ext, isNew, name, thisArg, args);
        if (!isNew && !enableCall) {
            if (name == null || name.equals("")) name = "(intermediate value)";
            throw EngineException.ofType(name + " is not invokable");
        }

        return onCall(ext, isNew, name, thisArg, args);
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

