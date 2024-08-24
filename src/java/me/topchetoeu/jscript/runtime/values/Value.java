package me.topchetoeu.jscript.runtime.values;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import me.topchetoeu.jscript.common.Operation;
// import me.topchetoeu.jscript.lib.PromiseLib;
import me.topchetoeu.jscript.runtime.debug.DebugContext;
import me.topchetoeu.jscript.runtime.environment.Environment;
import me.topchetoeu.jscript.runtime.exceptions.ConvertException;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.exceptions.SyntaxException;
import me.topchetoeu.jscript.utils.interop.NativeWrapperProvider;

public interface Value {
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

    public static final Object NULL = new Object();
    public static final Object NO_RETURN = new Object();

    public static double number(Object val) {
        if (val instanceof Number) return ((Number)val).doubleValue();
        else return Double.NaN;
    }

    @SuppressWarnings("unchecked")
    public static <T> T wrapper(Object val, Class<T> clazz) { 
        if (isWrapper(val)) val = ((NativeWrapper)val).wrapped;
        if (val != null && clazz.isInstance(val)) return (T)val;
        else return null;
    }

    public static String type(Object val) {
        if (val == null) return "undefined";
        if (val instanceof String) return "string";
        if (val instanceof Number) return "number";
        if (val instanceof Boolean) return "boolean";
        if (val instanceof Symbol) return "symbol";
        if (val instanceof FunctionValue) return "function";
        return "object";
    }

    public boolean isPrimitive();
    public BooleanValue toBoolean();

    public default Value call(Environment env, Value self, Value ...args) {
        throw EngineException.ofType("Tried to call a non-function value.");
    }
    public default Value callNew(Environment env, Value ...args) {
        var res = new ObjectValue();

        try {
            var proto = Values.getMember(env, this, "prototype");
            setPrototype(env, res, proto);

            var ret = this.call(env, res, args);

            if (!ret.isPrimitive()) return ret;
            return res;
        }
        catch (IllegalArgumentException e) {
            throw EngineException.ofType("Tried to call new on an invalid constructor.");
        }
    }

    public default Value toPrimitive(Environment env, Value val) {
        if (val.isPrimitive()) return val;

        if (env != null) {
            var valueOf = getMember(env, val, "valueOf");

            if (valueOf instanceof FunctionValue) {
                var res = valueOf.call(env, val);
                if (res.isPrimitive()) return res;
            }

            var toString = getMember(env, val, "toString");
            if (toString instanceof FunctionValue) {
                var res = toString.call(env, val);
                if (res.isPrimitive()) return res;
            }
        }

        throw EngineException.ofType("Value couldn't be converted to a primitive.");
    }
    public default NumberValue toNumber(Environment ext, Object obj) {
        var val = this.toPrimitive(ext, obj, ConvertHint.VALUEOF);

        if (val instanceof NumberValue) return number(val);
        if (val instanceof Boolean) return ((Boolean)val) ? 1 : 0;
        if (val instanceof String) {
            try { return Double.parseDouble((String)val); }
            catch (NumberFormatException e) { return Double.NaN; }
        }
        return Double.NaN;
    }
    public default StringValue toString(Environment ext, Object obj) {
        var val = toPrimitive(ext, obj, ConvertHint.VALUEOF);

        if (val == null) return "undefined";
        if (val == NULL) return "null";

        if (val instanceof Number) {
            var d = number(val);
            if (d == Double.NEGATIVE_INFINITY) return "-Infinity";
            if (d == Double.POSITIVE_INFINITY) return "Infinity";
            if (Double.isNaN(d)) return "NaN";
            return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
        }
        if (val instanceof Boolean) return (Boolean)val ? "true" : "false";
        if (val instanceof String) return (String)val;
        if (val instanceof Symbol) return val.toString();

        return "Unknown value";
    }

    public static Object add(Environment ext, Object a, Object b) {
        if (a instanceof String || b instanceof String) return toString(ext, a) + toString(ext, b);
        else return toNumber(ext, a) + toNumber(ext, b);
    }
    public static double subtract(Environment ext, Object a, Object b) {
        return toNumber(ext, a) - toNumber(ext, b);
    }
    public static double multiply(Environment ext, Object a, Object b) {
        return toNumber(ext, a) * toNumber(ext, b);
    }
    public static double divide(Environment ext, Object a, Object b) {
        return toNumber(ext, a) / toNumber(ext, b);
    }
    public static double modulo(Environment ext, Object a, Object b) {
        return toNumber(ext, a) % toNumber(ext, b);
    }
    
    public static double negative(Environment ext, Object obj) {
        return -toNumber(ext, obj);
    }

    public static int and(Environment ext, Object a, Object b) {
        return (int)toNumber(ext, a) & (int)toNumber(ext, b);
    }
    public static int or(Environment ext, Object a, Object b) {
        return (int)toNumber(ext, a) | (int)toNumber(ext, b);
    }
    public static int xor(Environment ext, Object a, Object b) {
        return (int)toNumber(ext, a) ^ (int)toNumber(ext, b);
    }
    public static int bitwiseNot(Environment ext, Object obj) {
        return ~(int)toNumber(ext, obj);
    }

    public static int shiftLeft(Environment ext, Object a, Object b) {
        return (int)toNumber(ext, a) << (int)toNumber(ext, b);
    }
    public static int shiftRight(Environment ext, Object a, Object b) {
        return (int)toNumber(ext, a) >> (int)toNumber(ext, b);
    }
    public static long unsignedShiftRight(Environment ext, Object a, Object b) {
        long _a = (long)toNumber(ext, a);
        long _b = (long)toNumber(ext, b);

        if (_a < 0) _a += 0x100000000l;
        if (_b < 0) _b += 0x100000000l;
        return _a >>> _b;
    }

    public static CompareResult compare(Environment ext, Object a, Object b) {
        a = toPrimitive(ext, a, ConvertHint.VALUEOF);
        b = toPrimitive(ext, b, ConvertHint.VALUEOF);

        if (a instanceof String && b instanceof String) CompareResult.from(((String)a).compareTo((String)b));

        var _a = toNumber(ext, a);
        var _b = toNumber(ext, b);

        if (Double.isNaN(_a) || Double.isNaN(_b)) return CompareResult.NOT_EQUAL;

        return CompareResult.from(Double.compare(_a, _b));
    }

    public static boolean not(Object obj) {
        return !toBoolean(obj);
    }

    public static boolean isInstanceOf(Environment ext, Object obj, Object proto) {
        if (obj == null || obj == NULL || proto == null || proto == NULL) return false;
        var val = getPrototype(ext, obj);

        while (val != null) {
            if (val.equals(proto)) return true;
            val = val.getPrototype(ext);
        }

        return false;
    }

    public static Object operation(Environment ext, Operation op, Object ...args) {
        switch (op) {
            case ADD: return add(ext, args[0], args[1]);
            case SUBTRACT: return subtract(ext, args[0], args[1]);
            case DIVIDE: return divide(ext, args[0], args[1]);
            case MULTIPLY: return multiply(ext, args[0], args[1]);
            case MODULO: return modulo(ext, args[0], args[1]);

            case AND: return and(ext, args[0], args[1]);
            case OR: return or(ext, args[0], args[1]);
            case XOR: return xor(ext, args[0], args[1]);

            case EQUALS: return strictEquals(ext, args[0], args[1]);
            case NOT_EQUALS: return !strictEquals(ext, args[0], args[1]);
            case LOOSE_EQUALS: return looseEqual(ext, args[0], args[1]);
            case LOOSE_NOT_EQUALS: return !looseEqual(ext, args[0], args[1]);

            case GREATER: return compare(ext, args[0], args[1]).greater();
            case GREATER_EQUALS: return compare(ext, args[0], args[1]).greaterOrEqual();
            case LESS: return compare(ext, args[0], args[1]).less();
            case LESS_EQUALS: return compare(ext, args[0], args[1]).lessOrEqual();

            case INVERSE: return bitwiseNot(ext, args[0]);
            case NOT: return not(args[0]);
            case POS: return toNumber(ext, args[0]);
            case NEG: return negative(ext, args[0]);

            case SHIFT_LEFT: return shiftLeft(ext, args[0], args[1]);
            case SHIFT_RIGHT: return shiftRight(ext, args[0], args[1]);
            case USHIFT_RIGHT: return unsignedShiftRight(ext, args[0], args[1]);

            case IN: return hasMember(ext, args[1], args[0], false);
            case INSTANCEOF: {
                var proto = getMember(ext, args[1], "prototype");
                return isInstanceOf(ext, args[0], proto);
            }

            default: return null;
        }
    }

    public static Object getMember(Environment ctx, Object obj, Object key) {
        obj = normalize(ctx, obj); key = normalize(ctx, key);
        if (obj == null) throw new IllegalArgumentException("Tried to access member of undefined.");
        if (obj == NULL) throw new IllegalArgumentException("Tried to access member of null.");
        if (obj instanceof ObjectValue) return ((ObjectValue)obj).getMember(ctx, key, obj);

        if (obj instanceof String && key instanceof Number) {
            var i = number(key);
            var s = (String)obj;
            if (i >= 0 && i < s.length() && i - Math.floor(i) == 0) {
                return s.charAt((int)i) + "";
            }
        }

        var proto = getPrototype(ctx, obj);

        if (proto == null) return "__proto__".equals(key) ? NULL : null;
        else if (key != null && "__proto__".equals(key)) return proto;
        else return proto.getMember(ctx, key, obj);
    }
    public static Object getMemberPath(Environment ctx, Object obj, Object ...path) {
        var res = obj;
        for (var key : path) res = getMember(ctx, res, key);
        return res;
    }
    public static boolean setMember(Environment ctx, Object obj, Object key, Object val) {
        obj = normalize(ctx, obj); key = normalize(ctx, key); val = normalize(ctx, val);
        if (obj == null) throw EngineException.ofType("Tried to access member of undefined.");
        if (obj == NULL) throw EngineException.ofType("Tried to access member of null.");
        if (key != null && "__proto__".equals(key)) return setPrototype(ctx, obj, val);
        if (obj instanceof ObjectValue) return ((ObjectValue)obj).setMember(ctx, key, val, obj, false);

        var proto = getPrototype(ctx, obj);
        return proto.setMember(ctx, key, val, obj, true);
    }
    public static boolean hasMember(Environment ctx, Object obj, Object key, boolean own) {
        if (obj == null || obj == NULL) return false;
        obj = normalize(ctx, obj); key = normalize(ctx, key);

        if ("__proto__".equals(key)) return true;
        if (obj instanceof ObjectValue) return ((ObjectValue)obj).hasMember(ctx, key, own);

        if (obj instanceof String && key instanceof Number) {
            var i = number(key);
            var s = (String)obj;
            if (i >= 0 && i < s.length() && i - Math.floor(i) == 0) return true;
        }

        if (own) return false;

        var proto = getPrototype(ctx, obj);
        return proto != null && proto.hasMember(ctx, key, own);
    }
    public static boolean deleteMember(Environment ext, Object obj, Object key) {
        if (obj == null || obj == NULL) return false;
        obj = normalize(ext, obj); key = normalize(ext, key);

        if (obj instanceof ObjectValue) return ((ObjectValue)obj).deleteMember(ext, key);
        else return false;
    }
    public static ObjectValue getPrototype(Environment ext, Object obj) {
        if (obj == null || obj == NULL) return null;
        obj = normalize(ext, obj);
        if (obj instanceof ObjectValue) return ((ObjectValue)obj).getPrototype(ext);
        if (ext == null) return null;

        if (obj instanceof String) return ext.get(Environment.STRING_PROTO);
        else if (obj instanceof Number) return ext.get(Environment.NUMBER_PROTO);
        else if (obj instanceof Boolean) return ext.get(Environment.BOOL_PROTO);
        else if (obj instanceof Symbol) return ext.get(Environment.SYMBOL_PROTO);

        return null;
    }
    public static boolean setPrototype(Environment ext, Object obj, Object proto) {
        obj = normalize(ext, obj);
        return obj instanceof ObjectValue && ((ObjectValue)obj).setPrototype(ext, proto);
    }
    public static void makePrototypeChain(Environment ext, Object... chain) {
        for(var i = 1; i < chain.length; i++) {
            setPrototype(ext, chain[i], chain[i - 1]);
        }
    }
    public static List<Object> getMembers(Environment ext, Object obj, boolean own, boolean includeNonEnumerable) {  
        List<Object> res = new ArrayList<>();

        if (obj instanceof ObjectValue) res = ((ObjectValue)obj).keys(includeNonEnumerable);
        if (obj instanceof String) {
            for (var i = 0; i < ((String)obj).length(); i++) res.add((double)i);
        }

        if (!own) {
            var proto = getPrototype(ext, obj);

            while (proto != null) {
                res.addAll(proto.keys(includeNonEnumerable));
                proto = getPrototype(ext, proto);
            }
        }


        return res;
    }
    public static ObjectValue getMemberDescriptor(Environment ext, Object obj, Object key) {
        if (obj instanceof ObjectValue) return ((ObjectValue)obj).getMemberDescriptor(ext, key);
        else if (obj instanceof String && key instanceof Number) {
            var i = ((Number)key).intValue();
            var _i = ((Number)key).doubleValue();
            if (i - _i != 0) return null;
            if (i < 0 || i >= ((String)obj).length()) return null;

            return new ObjectValue(ext, Map.of(
                "value", ((String)obj).charAt(i) + "",
                "writable", false,
                "enumerable", true,
                "configurable", false
            ));
        }
        else return null;
    }

    public static boolean strictEquals(Environment ext, Object a, Object b) {
        a = normalize(ext, a);
        b = normalize(ext, b);

        if (a == null || b == null) return a == null && b == null;
        if (isNan(a) || isNan(b)) return false;
        if (a instanceof Number && number(a) == -0.) a = 0.;
        if (b instanceof Number && number(b) == -0.) b = 0.;

        return a == b || a.equals(b);
    }
    public static boolean looseEqual(Environment ext, Object a, Object b) {
        a = normalize(ext, a); b = normalize(ext, b);

        // In loose equality, null is equivalent to undefined
        if (a == NULL) a = null;
        if (b == NULL) b = null;

        if (a == null || b == null) return a == null && b == null;
        // If both are objects, just compare their references
        if (!isPrimitive(a) && !isPrimitive(b)) return a == b;

        // Convert values to primitives
        a = toPrimitive(ext, a, ConvertHint.VALUEOF);
        b = toPrimitive(ext, b, ConvertHint.VALUEOF);

        // Compare symbols by reference
        if (a instanceof Symbol || b instanceof Symbol) return a == b;
        if (a instanceof Boolean || b instanceof Boolean) return toBoolean(a) == toBoolean(b);
        if (a instanceof Number || b instanceof Number) return strictEquals(ext, toNumber(ext, a), toNumber(ext, b));

        // Default to strings
        return toString(ext, a).equals(toString(ext, b));
    }

    public static Object normalize(Environment ext, Object val) {
        if (val instanceof Number) return number(val);
        if (isPrimitive(val) || val instanceof ObjectValue) return val;
        if (val instanceof Character) return val + "";

        if (val instanceof Map) {
            var res = new ObjectValue();

            for (var entry : ((Map<?, ?>)val).entrySet()) {
                res.defineProperty(ext, entry.getKey(), entry.getValue());
            }

            return res;
        }

        if (val instanceof Iterable) {
            var res = new ArrayValue();

            for (var entry : ((Iterable<?>)val)) {
                res.set(ext, res.size(), entry);
            }

            return res;
        }

        if (val instanceof Class) {
            if (ext == null) return null;
            else return NativeWrapperProvider.get(ext).getConstr((Class<?>)val);
        }

        return NativeWrapper.of(ext, val);
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(Environment ext, Object obj, Class<T> clazz) {
        if (clazz == Void.class) return null;

        if (obj instanceof NativeWrapper) {
            var res = ((NativeWrapper)obj).wrapped;
            if (clazz.isInstance(res)) return (T)res;
        }

        if (clazz == null || clazz == Object.class) return (T)obj;

        if (obj instanceof ArrayValue) {
            if (clazz.isAssignableFrom(ArrayList.class)) {
                var raw = ((ArrayValue)obj).toArray();
                var res = new ArrayList<>();
                for (var i = 0; i < raw.length; i++) res.add(convert(ext, raw[i], Object.class));
                return (T)new ArrayList<>(res);
            }
            if (clazz.isAssignableFrom(HashSet.class)) {
                var raw = ((ArrayValue)obj).toArray();
                var res = new HashSet<>();
                for (var i = 0; i < raw.length; i++) res.add(convert(ext, raw[i], Object.class));
                return (T)new HashSet<>(res);
            }
            if (clazz.isArray()) {
                var raw = ((ArrayValue)obj).toArray();
                Object res = Array.newInstance(clazz.getComponentType(), raw.length);
                for (var i = 0; i < raw.length; i++) Array.set(res, i, convert(ext, raw[i], Object.class));
                return (T)res;
            }
        }

        if (obj instanceof ObjectValue && clazz.isAssignableFrom(HashMap.class)) {
            var res = new HashMap<>();
            for (var el : ((ObjectValue)obj).values.entrySet()) res.put(
                convert(ext, el.getKey(), null),
                convert(ext, el.getValue(), null)
            );
            return (T)res;
        }

        if (clazz == String.class) return (T)toString(ext, obj);
        if (clazz == Boolean.class || clazz == Boolean.TYPE) return (T)(Boolean)toBoolean(obj);
        if (clazz == Byte.class || clazz == byte.class) return (T)(Byte)(byte)toNumber(ext, obj);
        if (clazz == Integer.class || clazz == int.class) return (T)(Integer)(int)toNumber(ext, obj);
        if (clazz == Long.class || clazz == long.class) return (T)(Long)(long)toNumber(ext, obj);
        if (clazz == Short.class || clazz == short.class) return (T)(Short)(short)toNumber(ext, obj);
        if (clazz == Float.class || clazz == float.class) return (T)(Float)(float)toNumber(ext, obj);
        if (clazz == Double.class || clazz == double.class) return (T)(Double)toNumber(ext, obj);

        if (clazz == Character.class || clazz == char.class) {
            if (obj instanceof Number) return (T)(Character)(char)number(obj);
            else {
                var res = toString(ext, obj);
                if (res.length() == 0) throw new ConvertException("\"\"", "Character");
                else return (T)(Character)res.charAt(0);
            }
        }

        if (obj == null) return null;
        if (clazz.isInstance(obj)) return (T)obj;
        if (clazz.isAssignableFrom(NativeWrapper.class)) {
            return (T)NativeWrapper.of(ext, obj);
        }

        throw new ConvertException(type(obj), clazz.getSimpleName());
    }

    public static Iterable<Object> fromJSIterator(Environment ext, Object obj) {
        return () -> {
            try {
                var symbol = Symbol.get("Symbol.iterator");

                var iteratorFunc = getMember(ext, obj, symbol);
                if (!(iteratorFunc instanceof FunctionValue)) return Collections.emptyIterator();
                var iterator = iteratorFunc instanceof FunctionValue ?
                    ((FunctionValue)iteratorFunc).call(ext, obj, obj) :
                    iteratorFunc;
                var nextFunc = getMember(ext, call(ext, iteratorFunc, obj), "next");

                if (!(nextFunc instanceof FunctionValue)) return Collections.emptyIterator();

                return new Iterator<Object>() {
                    private Object value = null;
                    public boolean consumed = true;
                    private FunctionValue next = (FunctionValue)nextFunc;

                    private void loadNext() {
                        if (next == null) value = null;
                        else if (consumed) {
                            var curr = next.call(ext, iterator);
                            if (curr == null) { next = null; value = null; }
                            if (toBoolean(Values.getMember(ext, curr, "done"))) { next = null; value = null; }
                            else {
                                this.value = Values.getMember(ext, curr, "value");
                                consumed = false;
                            }
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        loadNext();
                        return next != null;
                    }
                    @Override
                    public Object next() {
                        loadNext();
                        var res = value;
                        value = null;
                        consumed = true;
                        return res;
                    }
                };
            }
            catch (IllegalArgumentException | NullPointerException e) {
                return Collections.emptyIterator();
            }
        };
    }

    public static ObjectValue toJSIterator(Environment ext, Iterator<?> it) {
        var res = new ObjectValue();

        try {
            var key = getMember(ext, getMember(ext, ext.get(Environment.SYMBOL_PROTO), "constructor"), "iterator");
            res.defineProperty(ext, key, new NativeFunction("", args -> args.self));
        }
        catch (IllegalArgumentException | NullPointerException e) { }

        res.defineProperty(ext, "next", new NativeFunction("", args -> {
            if (!it.hasNext()) return new ObjectValue(ext, Map.of("done", true));
            else {
                var obj = new ObjectValue();
                obj.defineProperty(args.env, "value", it.next());
                return obj;
            }
        }));

        return res;
    }

    public static ObjectValue toJSIterator(Environment ext, Iterable<?> it) {
        return toJSIterator(ext, it.iterator());
    }

    public static ObjectValue toJSAsyncIterator(Environment ext, Iterator<?> it) {
        var res = new ObjectValue();

        try {
            var key = getMemberPath(ext, ext.get(Environment.SYMBOL_PROTO), "constructor", "asyncIterator");
            res.defineProperty(ext, key, new NativeFunction("", args -> args.self));
        }
        catch (IllegalArgumentException | NullPointerException e) { }

        res.defineProperty(ext, "next", new NativeFunction("", args -> {
            return PromiseLib.await(args.env, () -> {
                if (!it.hasNext()) return new ObjectValue(ext, Map.of("done", true));
                else {
                    var obj = new ObjectValue();
                    obj.defineProperty(args.env, "value", it.next());
                    return obj;
                }
            });
        }));

        return res;
    }

    private static boolean isEmptyFunc(ObjectValue val) {
        if (!(val instanceof FunctionValue)) return false;
        if (!val.values.containsKey("prototype") || val.values.size() + val.properties.size() > 1) return false;
        var proto = val.values.get("prototype");
        if (!(proto instanceof ObjectValue)) return false;
        var protoObj = (ObjectValue)proto;
        if (protoObj.values.get("constructor") != val) return false;
        if (protoObj.values.size() + protoObj.properties.size() != 1) return false;
        return true;
    }
    private static String toReadable(Environment ext, Object val, HashSet<Object> passed, int tab) {
        if (tab == 0 && val instanceof String) return (String)val;

        if (passed.contains(val)) return "[circular]";

        var printed = true;
        var res = new StringBuilder();
        var dbg = DebugContext.get(ext);

        if (val instanceof FunctionValue) {
            res.append(val.toString());
            var loc = val instanceof CodeFunction ? dbg.getMapOrEmpty((CodeFunction)val).start() : null;

            if (loc != null) res.append(" @ " + loc);
        }
        else if (val instanceof ArrayValue) {
            res.append("[");
            var obj = ((ArrayValue)val);
            for (int i = 0; i < obj.size(); i++) {
                if (i != 0) res.append(", ");
                else res.append(" ");
                if (obj.has(i)) res.append(toReadable(ext, obj.get(i), passed, tab));
                else res.append("<empty>");
            }
            res.append(" ] ");
        }
        else if (val instanceof NativeWrapper) {
            var obj = ((NativeWrapper)val).wrapped;
            res.append("Native " + obj.toString() + " ");
        }
        else printed = false;

        if (val instanceof ObjectValue) {
            if (tab > 3) {
                return "{...}";
            }

            passed.add(val);

            var obj = (ObjectValue)val;
            if (obj.values.size() + obj.properties.size() == 0 || isEmptyFunc(obj)) {
                if (!printed) res.append("{}\n");
            }
            else {
                res.append("{\n");

                for (var el : obj.values.entrySet()) {
                    for (int i = 0; i < tab + 1; i++) res.append("    ");
                    res.append(toReadable(ext, el.getKey(), passed, tab + 1));
                    res.append(": ");
                    res.append(toReadable(ext, el.getValue(), passed, tab + 1));
                    res.append(",\n");
                }
                for (var el : obj.properties.entrySet()) {
                    for (int i = 0; i < tab + 1; i++) res.append("    ");
                    res.append(toReadable(ext, el.getKey(), passed, tab + 1));
                    res.append(": [prop],\n");
                }

                for (int i = 0; i < tab; i++) res.append("    ");
                res.append("}");
            }

            passed.remove(val);
        }
        else if (val == null) return "undefined";
        else if (val == Values.NULL) return "null";
        else if (val instanceof String) return "'" + val + "'";
        else return Values.toString(ext, val);

        return res.toString();
    }

    public static String toReadable(Environment ext, Object val) {
        return toReadable(ext, val, new HashSet<>(), 0);
    }
    public static String errorToReadable(RuntimeException err, String prefix) {
        prefix = prefix == null ? "Uncaught" : "Uncaught " + prefix;
        if (err instanceof EngineException) {
            var ee = ((EngineException)err);
            try {
                return prefix + " " + ee.toString(ee.env);
            }
            catch (EngineException ex) {
                return prefix + " " + toReadable(ee.env, ee.value);
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
    public static void printValue(Environment ext, Object val) {
        System.out.print(toReadable(ext, val));
    }
    public static void printError(RuntimeException err, String prefix) {
        System.out.println(errorToReadable(err, prefix));
    }
}
