package me.topchetoeu.jscript.runtime.values.functions;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.runtime.values.KeyCache;
import me.topchetoeu.jscript.runtime.values.Member;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.NumberValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;

public abstract class FunctionValue extends ObjectValue {
    public String name = "";
    public int length;
    public Value prototype = new ObjectValue();

    public boolean enableCall = true;
    public boolean enableNew = true;

    private final FieldMember nameField = new FieldMember(true, false, false) {
        @Override public Value get(Environment env, Value self) {
            if (name == null) return new StringValue("");
            return new StringValue(name);
        }
        @Override public boolean set(Environment env, Value val, Value self) {
            name = val.toString(env).value;
            return true;
        }
    };
    private final FieldMember lengthField = new FieldMember(true, false, false) {
        @Override public Value get(Environment env, Value self) {
            return new NumberValue(length);
        }
        @Override public boolean set(Environment env, Value val, Value self) {
            return false;
        }
    };
    private final FieldMember prototypeField = new FieldMember(false, false, true) {
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
        if (!isNew && !enableCall) super.call(ext, isNew, name, thisArg, args);

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

    public void setName(String val) {
        if (this.name == null || this.name.equals("")) this.name = val;
    }

    public FunctionValue(String name, int length) {
        setPrototype(FUNCTION_PROTO);

        if (name == null) name = "";
        this.length = length;
        this.name = name;

        prototype.defineOwnMember(null, "constructor", FieldMember.of(this));
    }
}

