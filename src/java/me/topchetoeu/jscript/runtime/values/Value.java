package me.topchetoeu.jscript.runtime.values;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.environment.Key;
import me.topchetoeu.jscript.common.json.JSON;
import me.topchetoeu.jscript.common.json.JSONElement;
import me.topchetoeu.jscript.runtime.EventLoop;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.functions.CodeFunction;
import me.topchetoeu.jscript.runtime.values.functions.FunctionValue;
import me.topchetoeu.jscript.runtime.values.functions.NativeFunction;
import me.topchetoeu.jscript.runtime.values.objects.ArrayValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.BoolValue;
import me.topchetoeu.jscript.runtime.values.primitives.NumberValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.SymbolValue;
import me.topchetoeu.jscript.runtime.values.primitives.VoidValue;

public abstract class Value {
    public static enum CompareResult {
        NOT_EQUAL,
        EQUAL,
        LESS,
        GREATER;

        public boolean less() { return this == LESS; }
        public boolean greater() { return this == GREATER; }
        public boolean lessOrEqual() { return this == LESS || this == EQUAL; }
        public boolean greaterOrEqual() { return this == GREATER || this == EQUAL; }

        public static CompareResult from(int cmp) {
            if (cmp < 0) return LESS;
            if (cmp > 0) return GREATER;
            return EQUAL;
        }
    }

    public static final Key<FunctionValue> REGEX_CONSTR = Key.of();

    public static final Key<Integer> MAX_STACK_COUNT = Key.of();
    public static final Key<Boolean> HIDE_STACK = Key.of();
    public static final Key<ObjectValue> OBJECT_PROTO = Key.of();
    public static final Key<ObjectValue> FUNCTION_PROTO = Key.of();
    public static final Key<ObjectValue> ARRAY_PROTO = Key.of();
    public static final Key<ObjectValue> BOOL_PROTO = Key.of();
    public static final Key<ObjectValue> NUMBER_PROTO = Key.of();
    public static final Key<ObjectValue> STRING_PROTO = Key.of();
    public static final Key<ObjectValue> SYMBOL_PROTO = Key.of();
    public static final Key<ObjectValue> ERROR_PROTO = Key.of();
    public static final Key<ObjectValue> SYNTAX_ERR_PROTO = Key.of();
    public static final Key<ObjectValue> TYPE_ERR_PROTO = Key.of();
    public static final Key<ObjectValue> RANGE_ERR_PROTO = Key.of();
    public static final Key<ObjectValue> GLOBAL = Key.of();

    public static final VoidValue UNDEFINED = new VoidValue("undefined", new StringValue("undefined"));
    public static final VoidValue NULL = new VoidValue("null", new StringValue("object"));

    public abstract StringValue type();
    public abstract boolean isPrimitive();

    public final boolean isNaN() {
        return this instanceof NumberValue && Double.isNaN(((NumberValue)this).value);
    }

    public Value call(Environment env, boolean isNew, String name, Value self, Value ...args) {
        if (name == null || name.equals("")) name = "(intermediate value)";

        if (isNew) throw EngineException.ofType(name + " is not a constructor");
        else throw EngineException.ofType(name + " is not a function");
    }
    public final Value callNew(Environment env, String name, Value ...args) {
        var res = new ObjectValue();
        var proto = getMember(env, new StringValue("prototype"));

        if (proto instanceof ObjectValue) res.setPrototype(env, (ObjectValue)proto);
        else res.setPrototype(env, null);

        var ret = this.call(env, true, name, res, args);

        if (!ret.isPrimitive()) return ret;
        return res;
    }

    public final Value call(Environment env, Value self, Value ...args) {
        return call(env, false, "", self, args);
    }
    public final Value callNew(Environment env, Value ...args) {
        return callNew(env, "", args);
    }

    public abstract Value toPrimitive(Environment env);
    public abstract NumberValue toNumber(Environment env);
    public abstract StringValue toString(Environment env);
    public abstract boolean toBoolean();

    public final int toInt(Environment env) { return (int)toNumber(env).value; }
    public final long toLong(Environment env) { return (long)toNumber(env).value; }

    public Value add(Environment env, Value other) {
        if (this instanceof StringValue || other instanceof StringValue) {
            return new StringValue(this.toString(env).value + other.toString(env).value);
        }
        else return new NumberValue(this.toNumber(env).value + other.toNumber(env).value);
    }
    public NumberValue subtract(Environment env, Value other) {
        return new NumberValue(toNumber(env).value - other.toNumber(env).value);
    }
    public NumberValue multiply(Environment env, Value other) {
        return new NumberValue(toNumber(env).value - other.toNumber(env).value);
    }
    public NumberValue divide(Environment env, Value other) {
        return new NumberValue(toNumber(env).value / other.toNumber(env).value);
    }
    public NumberValue modulo(Environment env, Value other) {
        return new NumberValue(toNumber(env).value % other.toNumber(env).value);
    }
    public NumberValue negative(Environment env) {
        return new NumberValue(-toNumber(env).value);
    }

    public NumberValue and(Environment env, Value other) {
        return new NumberValue(this.toInt(env) & other.toInt(env));
    }
    public NumberValue or(Environment env, Value other) {
        return new NumberValue(this.toInt(env) | other.toInt(env));
    }
    public NumberValue xor(Environment env, Value other) {
        return new NumberValue(this.toInt(env) ^ other.toInt(env));
    }
    public NumberValue bitwiseNot(Environment env) {
        return new NumberValue(~this.toInt(env));
    }

    public NumberValue shiftLeft(Environment env, Value other) {
        return new NumberValue(this.toInt(env) << other.toInt(env));
    }
    public NumberValue shiftRight(Environment env, Value other) {
        return new NumberValue(this.toInt(env) >> other.toInt(env));
    }
    public NumberValue unsignedShiftRight(Environment env, Value other) {
        long a = this.toInt(env);
        long b = other.toInt(env);

        if (a < 0) a += 0x100000000l;
        if (b < 0) b += 0x100000000l;

        return new NumberValue(a >>> b);
    }

    public CompareResult compare(Environment env, Value other) {
        var a = this.toPrimitive(env);
        var b = other.toPrimitive(env);

        if (a instanceof StringValue && b instanceof StringValue) return a.compare(env, b);
        else return a.toNumber(env).compare(env, b.toNumber(env));
    }

    public final boolean isInstanceOf(Environment env, Value proto) {
        for (var val = getPrototype(env); val != null; val = getPrototype(env)) {
            if (val.equals(proto)) return true;
        }

        return false;
    }

    public static Value operation(Environment env, Operation op, Value ...args) {
        switch (op) {
            case ADD: return args[0].add(env, args[1]);
            case SUBTRACT: return args[0].subtract(env, args[1]);
            case DIVIDE: return args[0].divide(env, args[1]);
            case MULTIPLY: return args[0].multiply(env, args[1]);
            case MODULO: return args[0].modulo(env, args[1]);

            case AND: return args[0].and(env, args[1]);
            case OR: return args[0].or(env, args[1]);
            case XOR: return args[0].xor(env, args[1]);

            case EQUALS: return BoolValue.of(args[0].strictEquals(env, args[1]));
            case NOT_EQUALS: return BoolValue.of(!args[0].strictEquals(env, args[1]));
            case LOOSE_EQUALS: return BoolValue.of(args[0].looseEqual(env, args[1]));
            case LOOSE_NOT_EQUALS: return BoolValue.of(!args[0].looseEqual(env, args[1]));

            case GREATER: return BoolValue.of(args[0].compare(env, args[1]).greater());
            case GREATER_EQUALS: return BoolValue.of(args[0].compare(env, args[1]).greaterOrEqual());
            case LESS: return BoolValue.of(args[0].compare(env, args[1]).less());
            case LESS_EQUALS: return BoolValue.of(args[0].compare(env, args[1]).lessOrEqual());

            case INVERSE: return args[0].bitwiseNot(env);
            case NOT: return BoolValue.of(!args[0].toBoolean());
            case POS: return args[0].toNumber(env);
            case NEG: return args[0].negative(env);

            case SHIFT_LEFT: return args[0].shiftLeft(env, args[1]);
            case SHIFT_RIGHT: return args[0].shiftRight(env, args[1]);
            case USHIFT_RIGHT: return args[0].unsignedShiftRight(env, args[1]);

            case IN: return BoolValue.of(args[0].hasMember(env, args[1], false));
            case INSTANCEOF: return BoolValue.of(args[0].isInstanceOf(env, args[1].getMember(env, new StringValue("prototype"))));

            default: return null;
        }
    }

    public abstract Member getOwnMember(Environment env, KeyCache key);
    public abstract Map<String, Member> getOwnMembers(Environment env);
    public abstract Map<SymbolValue, Member> getOwnSymbolMembers(Environment env);
    public abstract boolean defineOwnMember(Environment env, KeyCache key, Member member);
    public abstract boolean deleteOwnMember(Environment env, KeyCache key);

    public abstract ObjectValue getPrototype(Environment env);
    public abstract boolean setPrototype(Environment env, ObjectValue val);

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

    public final boolean defineOwnMember(Environment env, Value key, Member member) {
        return defineOwnMember(env, new KeyCache(key), member);
    }
    public final boolean defineOwnMember(Environment env, String key, Member member) {
        return defineOwnMember(env, new KeyCache(key), member);
    }
    public final boolean defineOwnMember(Environment env, int key, Member member) {
        return defineOwnMember(env, new KeyCache(key), member);
    }
    public final boolean defineOwnMember(Environment env, double key, Member member) {
        return defineOwnMember(env, new KeyCache(key), member);
    }

    public final boolean defineOwnMember(Environment env, KeyCache key, Value val) {
        return defineOwnMember(env, key, FieldMember.of(val));
    }
    public final boolean defineOwnMember(Environment env, Value key, Value val) {
        return defineOwnMember(env, new KeyCache(key), FieldMember.of(val));
    }
    public final boolean defineOwnMember(Environment env, String key, Value val) {
        return defineOwnMember(env, new KeyCache(key), FieldMember.of(val));
    }
    public final boolean defineOwnMember(Environment env, int key, Value val) {
        return defineOwnMember(env, new KeyCache(key), FieldMember.of(val));
    }
    public final boolean defineOwnMember(Environment env, double key, Value val) {
        return defineOwnMember(env, new KeyCache(key), FieldMember.of(val));
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
            if (member != null) return member.get(env, obj);
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
            if (member != null) {
                if (member.set(env, val, obj)) {
                    if (val instanceof FunctionValue) ((FunctionValue)val).setName(key.toString(env));
                    return true;
                }
                else return false;
            }
        }

        if (defineOwnMember(env, key, FieldMember.of(val))) {
            if (val instanceof FunctionValue) ((FunctionValue)val).setName(key.toString(env));
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
                    if (val instanceof FunctionValue) ((FunctionValue)val).setName(key.toString(env));
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

    public final Map<String, Member> getMembers(Environment env, boolean own, boolean onlyEnumerable) {
        var res = new LinkedHashMap<String, Member>();
        var protos = new ArrayList<Value>();

        for (var proto = this; proto != null; proto = proto.getPrototype(env)) {
            protos.add(proto);
            if (own) break;
        }

        Collections.reverse(protos);

        for (var proto : protos) {
            if (onlyEnumerable) {
                for (var el : proto.getOwnMembers(env).entrySet()) {
                    if (!el.getValue().enumerable()) continue;
                    res.put(el.getKey(), el.getValue());
                }
            }
            else res.putAll(proto.getOwnMembers(env));
        }

        return res;
    }
    public final Map<SymbolValue, Member> getSymbolMembers(Environment env, boolean own, boolean onlyEnumerable) {
        var res = new LinkedHashMap<SymbolValue, Member>();
        var protos = new ArrayList<Value>();

        for (var proto = this; proto != null; proto = proto.getPrototype(env)) {
            protos.add(proto);
            if (own) break;
        }

        Collections.reverse(protos);

        for (var proto : protos) {
            if (onlyEnumerable) {
                for (var el : proto.getOwnSymbolMembers(env).entrySet()) {
                    if (!el.getValue().enumerable()) continue;
                    res.put(el.getKey(), el.getValue());
                }
            }
            else res.putAll(proto.getOwnSymbolMembers(env));
        }

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

    public abstract boolean strictEquals(Environment env, Value other);

    public final boolean looseEqual(Environment env, Value other) {
        var a = this;
        var b = other;

        // In loose equality, null is equivalent to undefined
        if (a instanceof VoidValue || b instanceof VoidValue) return a instanceof VoidValue && b instanceof VoidValue;

        // If both are objects, just compare their references
        if (!a.isPrimitive() && !b.isPrimitive()) return a.strictEquals(env, b);

        // Convert values to primitives
        a = a.toPrimitive(env);
        b = b.toPrimitive(env);

        // Compare symbols by reference
        if (a instanceof SymbolValue || b instanceof SymbolValue) return a.strictEquals(env, b);
        // Compare booleans as numbers
        if (a instanceof BoolValue || b instanceof BoolValue) return a.toNumber(env).strictEquals(env, b.toNumber(env));
        // Comparse numbers as numbers
        if (a instanceof NumberValue || b instanceof NumberValue) return a.toNumber(env).strictEquals(env, b.toNumber(env));

        // Default to strings
        return a.toString(env).strictEquals(env, b.toString(env));
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
                        var curr = supplier.call(env, Value.UNDEFINED);

                        if (curr == null) { supplier = null; value = null; }
                        if (curr.getMember(env, new StringValue("done")).toBoolean()) { supplier = null; value = null; }
                        else {
                            this.value = curr.getMember(env, new StringValue("value"));
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

    public static FunctionValue fromIterator(Environment ext, Iterable<? extends Value> iterable) {
        var it = iterable.iterator();

        return new NativeFunction("", args -> {
            var obj = new ObjectValue();

            if (!it.hasNext()) obj.defineOwnMember(args.env, "done", FieldMember.of(BoolValue.TRUE));
            else obj.defineOwnMember(args.env, "value", FieldMember.of(it.next()));

            return obj;
        });
    }

    public void callWith(Environment env, Iterable<? extends Value> it) {
        for (var el : it) {
            this.call(env, Value.UNDEFINED, el);
        }
    }
    public void callWithAsync(Environment env, Iterable<? extends Value> it, boolean async) {
        for (var el : it) {
            env.get(EventLoop.KEY).pushMsg(() -> this.call(env, Value.UNDEFINED, el), true);
        }
    }

    private final boolean isEmptyFunc(Environment env, ObjectValue val) {
        if (!(val instanceof FunctionValue)) return false;
        if (val.members.size() + val.symbolMembers.size() > 1) return false;

        var proto = ((FunctionValue)val).prototype;
        if (!(proto instanceof ObjectValue)) return false;
        var protoObj = (ObjectValue)proto;

        if (protoObj.getMember(env, new StringValue("constructor")) != val) return false;
        if (protoObj.getOwnMembers(env).size() + protoObj.getOwnSymbolMembers(env).size() != 1) return false;

        return true;
    }
    private final String toReadable(Environment env, HashSet<Object> passed, int tab) {
        if (passed.contains(this)) return "[circular]";

        if (this instanceof ObjectValue) {
            var res = new StringBuilder();
            var dbg = DebugContext.get(env);
            var printed = true;

            if (this instanceof FunctionValue) {
                res.append(this.toString());
                var loc = this instanceof CodeFunction ? dbg.getMapOrEmpty((CodeFunction)this).start() : null;

                if (loc != null) res.append(" @ " + loc);
            }
            else if (this instanceof ArrayValue) {
                res.append("[");
                var arr = (ArrayValue)this;

                for (int i = 0; i < arr.size(); i++) {
                    if (i != 0) res.append(", ");
                    else res.append(" ");
                    if (arr.has(i)) res.append(arr.get(i).toReadable(env, passed, tab));
                    else res.append("<empty>");
                }

                res.append(" ] ");
            }
            else printed = false;

            if (tab > 3) return "{...}";

            passed.add(this);

            var obj = (ObjectValue)this;
            if (obj.getOwnSymbolMembers(env).size() + obj.getOwnMembers(env).size() == 0 || isEmptyFunc(env, obj)) {
                if (!printed) res.append("{}\n");
            }
            else {
                res.append("{\n");

                for (var entry : obj.getOwnSymbolMembers(env).entrySet()) {
                    for (int i = 0; i < tab + 1; i++) res.append("    ");
                    res.append("[" + entry.getKey().value + "]" + ": ");

                    var member = entry.getValue();
                    if (member instanceof FieldMember) res.append(((FieldMember)member).get(env, obj).toReadable(env, passed, tab + 1));
                    else res.append("[property]");

                    res.append(",\n");
                }
                for (var entry : obj.getOwnMembers(env).entrySet()) {
                    for (int i = 0; i < tab + 1; i++) res.append("    ");
                    res.append(entry.getKey() + ": ");

                    var member = entry.getValue();
                    if (member instanceof FieldMember) res.append(((FieldMember)member).get(env, obj).toReadable(env, passed, tab + 1));
                    else res.append("[property]");

                    res.append(",\n");
                }

                for (int i = 0; i < tab; i++) res.append("    ");
                res.append("}");
            }

            passed.remove(this);
            return res.toString();
        }
        else if (this instanceof VoidValue) return ((VoidValue)this).name;
        else if (this instanceof StringValue) return JSON.stringify(JSONElement.string(((StringValue)this).value));
        else if (this instanceof SymbolValue) return this.toString();
        else return this.toString(env).value;
    }

    public final String toReadable(Environment ext) {
        return toReadable(ext, new HashSet<>(), 0);
    }

    public static final String errorToReadable(RuntimeException err, String prefix) {
        prefix = prefix == null ? "Uncaught" : "Uncaught " + prefix;
        if (err instanceof EngineException) {
            var ee = ((EngineException)err);
            try {
                return prefix + " " + ee.toString(ee.env);
            }
            catch (EngineException ex) {
                return prefix + " " + ee.value.toReadable(ee.env);
            }
        }
        else if (err instanceof SyntaxException) {
            return prefix + " SyntaxError " + ((SyntaxException)err).msg;
        }
        else if (err.getCause() instanceof InterruptedException) return "";
        else {
            var str = new ByteArrayOutputStream();
            err.printStackTrace(new PrintStream(str));

            return prefix + " internal error " + str.toString();
        }
    }

    public static final ObjectValue global(Environment env) {
        return env.initFrom(GLOBAL, () -> new ObjectValue());
    }
}
