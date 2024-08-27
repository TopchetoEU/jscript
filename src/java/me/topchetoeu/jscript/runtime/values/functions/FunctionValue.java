package me.topchetoeu.jscript.runtime.values.functions;

import me.topchetoeu.jscript.runtime.environment.Environment;
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

    private final FieldMember nameField = new FieldMember(false, true, true) {
        @Override public Value get(Environment env, Value self) {
            return new StringValue(name);
        }
        @Override public boolean set(Environment env, Value val, Value self) {
            name = val.toString(env).value;
            return true;
        }
    };
    private final FieldMember lengthField = new FieldMember(false, true, false) {
        @Override public Value get(Environment env, Value self) {
            return new NumberValue(length);
        }
        @Override public boolean set(Environment env, Value val, Value self) {
            return false;
        }
    };
    private final FieldMember prototypeField = new FieldMember(false, true, true) {
        @Override public Value get(Environment env, Value self) {
            return prototype;
        }
        @Override public boolean set(Environment env, Value val, Value self) {
            prototype = val;
            return true;
        }
    };

    @Override public String toString() { return String.format("function %s(...)", name); }
    @Override public abstract Value call(Environment ext, Value thisArg, Value ...args);

    @Override public Member getOwnMember(Environment env, Value key) {
        var el = key.toString(env).value;

        if (el.equals("length")) return lengthField;
        if (el.equals("name")) return nameField;
        if (el.equals("prototype")) return prototypeField;

        return super.getOwnMember(env, key);
    }
    @Override public boolean deleteOwnMember(Environment env, Value key) {
        if (!super.deleteOwnMember(env, key)) return false;

        var el = key.toString(env).value;

        if (el.equals("length")) return false;
        if (el.equals("name")) return false;
        if (el.equals("prototype")) return false;

        return true;
    }

    public FunctionValue(String name, int length) {
        setPrototype(Environment.FUNCTION_PROTO);

        if (name == null) name = "";
        this.length = length;
        this.name = name;

        prototype.defineOwnMember(Environment.empty(), new StringValue("constructor"), FieldMember.of(this));
    }
}

