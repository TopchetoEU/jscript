package me.topchetoeu.jscript.engine.values;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.frame.ConvertHint;
import me.topchetoeu.jscript.exceptions.EngineException;

public class Values {
    public static final Object NULL = new Object();

    public static boolean isObject(Object val) { return val instanceof ObjectValue; }
    public static boolean isFunction(Object val) { return val instanceof FunctionValue; }
    public static boolean isArray(Object val) { return val instanceof ArrayValue; }
    public static boolean isWrapper(Object val) { return val instanceof NativeWrapper; }
    public static boolean isWrapper(Object val, Class<?> clazz) {
        if (!isWrapper(val)) return false;
        var res = (NativeWrapper)val;
        return res != null && clazz.isInstance(res.wrapped);
    }
    public static boolean isNan(Object val) { return val instanceof Number && Double.isNaN(number(val)); }

    public static ObjectValue object(Object val) {
        if (val instanceof ObjectValue) return (ObjectValue)val;
        else return null;
    }
    public static ArrayValue array(Object val) {
        if (val instanceof ArrayValue) return (ArrayValue)val;
        else return null;
    }
    public static FunctionValue function(Object val) {
        if (val instanceof FunctionValue) return (FunctionValue)val;
        else return null;
    }
    public static double number(Object val) {
        if (val instanceof Number) return ((Number)val).doubleValue();
        else return Double.NaN;
    }

    @SuppressWarnings("unchecked")
    public static <T> T wrapper(Object val, Class<T> clazz) { 
        if (!isWrapper(val)) return null;

        var res = (NativeWrapper)val;
        if (res != null && clazz.isInstance(res.wrapped)) return (T)res.wrapped;
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

    private static Object tryCallConvertFunc(Context ctx, Object obj, String name) throws InterruptedException {
        var func = getMember(ctx, obj, name);

        if (func != null) {
            var res = ((FunctionValue)func).call(ctx, obj);
            if (isPrimitive(res)) return res;
        }

        throw EngineException.ofType("Value couldn't be converted to a primitive.");
    }

    public static boolean isPrimitive(Object obj) {
        return
            obj instanceof Number ||
            obj instanceof String ||
            obj instanceof Boolean ||
            obj instanceof Symbol ||
            obj instanceof SignalValue ||
            obj == null ||
            obj == NULL;
    }

    public static Object toPrimitive(Context ctx, Object obj, ConvertHint hint) throws InterruptedException {
        obj = normalize(ctx, obj);
        if (isPrimitive(obj)) return obj;

        var first = hint == ConvertHint.VALUEOF ? "valueOf" : "toString";
        var second = hint == ConvertHint.VALUEOF ? "toString" : "valueOf";

        if (ctx != null) {
            try {
                return tryCallConvertFunc(ctx, obj, first);
            }
            catch (EngineException unused) {
                return tryCallConvertFunc(ctx, obj, second);
            }
        }

        throw EngineException.ofType("Value couldn't be converted to a primitive.");
    }
    public static boolean toBoolean(Object obj) {
        if (obj == NULL || obj == null) return false;
        if (obj instanceof Number && number(obj) == 0) return false;
        if (obj instanceof String && ((String)obj).equals("")) return false;
        if (obj instanceof Boolean) return (Boolean)obj;
        return true;
    }
    public static double toNumber(Context ctx, Object obj) throws InterruptedException {
        var val = toPrimitive(ctx, obj, ConvertHint.VALUEOF);

        if (val instanceof Number) return number(obj);
        if (val instanceof Boolean) return ((Boolean)obj) ? 1 : 0;
        if (val instanceof String) {
            try {
                return Double.parseDouble((String)val);
            }
            catch (NumberFormatException e) { }
        }
        return Double.NaN;
    }
    public static String toString(Context ctx, Object obj) throws InterruptedException {
        var val = toPrimitive(ctx, obj, ConvertHint.VALUEOF);

        if (val == null) return "undefined";
        if (val == NULL) return "null";

        if (val instanceof Number) {
            var d = number(obj);
            if (d == Double.NEGATIVE_INFINITY) return "-Infinity";
            if (d == Double.POSITIVE_INFINITY) return "Infinity";
            if (Double.isNaN(d)) return "NaN";
            return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
        }
        if (val instanceof Boolean) return (Boolean)val ? "true" : "false";
        if (val instanceof String) return (String)val;
        if (val instanceof Symbol) return ((Symbol)val).toString();
        if (val instanceof SignalValue) return "[signal '" + ((SignalValue)val).data + "']";

        return "Unknown value";
    }

    public static Object add(Context ctx, Object a, Object b) throws InterruptedException {
        if (a instanceof String || b instanceof String) return toString(ctx, a) + toString(ctx, b);
        else return toNumber(ctx, a) + toNumber(ctx, b);
    }
    public static double subtract(Context ctx, Object a, Object b) throws InterruptedException {
        return toNumber(ctx, a) - toNumber(ctx, b);
    }
    public static double multiply(Context ctx, Object a, Object b) throws InterruptedException {
        return toNumber(ctx, a) * toNumber(ctx, b);
    }
    public static double divide(Context ctx, Object a, Object b) throws InterruptedException {
        return toNumber(ctx, a) / toNumber(ctx, b);
    }
    public static double modulo(Context ctx, Object a, Object b) throws InterruptedException {
        return toNumber(ctx, a) % toNumber(ctx, b);
    }
    
    public static double negative(Context ctx, Object obj) throws InterruptedException {
        return -toNumber(ctx, obj);
    }

    public static int and(Context ctx, Object a, Object b) throws InterruptedException {
        return (int)toNumber(ctx, a) & (int)toNumber(ctx, b);
    }
    public static int or(Context ctx, Object a, Object b) throws InterruptedException {
        return (int)toNumber(ctx, a) | (int)toNumber(ctx, b);
    }
    public static int xor(Context ctx, Object a, Object b) throws InterruptedException {
        return (int)toNumber(ctx, a) ^ (int)toNumber(ctx, b);
    }
    public static int bitwiseNot(Context ctx, Object obj) throws InterruptedException {
        return ~(int)toNumber(ctx, obj);
    }

    public static int shiftLeft(Context ctx, Object a, Object b) throws InterruptedException {
        return (int)toNumber(ctx, a) << (int)toNumber(ctx, b);
    }
    public static int shiftRight(Context ctx, Object a, Object b) throws InterruptedException {
        return (int)toNumber(ctx, a) >> (int)toNumber(ctx, b);
    }
    public static long unsignedShiftRight(Context ctx, Object a, Object b) throws InterruptedException {
        long _a = (long)toNumber(ctx, a);
        long _b = (long)toNumber(ctx, b);

        if (_a < 0) _a += 0x100000000l;
        if (_b < 0) _b += 0x100000000l;
        return _a >>> _b;
    }

    public static int compare(Context ctx, Object a, Object b) throws InterruptedException {
        a = toPrimitive(ctx, a, ConvertHint.VALUEOF);
        b = toPrimitive(ctx, b, ConvertHint.VALUEOF);

        if (a instanceof String && b instanceof String) return ((String)a).compareTo((String)b);
        else return Double.compare(toNumber(ctx, a), toNumber(ctx, b));
    }

    public static boolean not(Object obj) {
        return !toBoolean(obj);
    }

    public static boolean isInstanceOf(Context ctx, Object obj, Object proto) throws InterruptedException {
        if (obj == null || obj == NULL || proto == null || proto == NULL) return false;
        var val = getPrototype(ctx, obj);

        while (val != null) {
            if (val.equals(proto)) return true;
            val = val.getPrototype(ctx);
        }

        return false;
    }

    public static Object operation(Context ctx, Operation op, Object ...args) throws InterruptedException {
        switch (op) {
            case ADD: return add(ctx, args[0], args[1]);
            case SUBTRACT: return subtract(ctx, args[0], args[1]);
            case DIVIDE: return divide(ctx, args[0], args[1]);
            case MULTIPLY: return multiply(ctx, args[0], args[1]);
            case MODULO: return modulo(ctx, args[0], args[1]);

            case AND: return and(ctx, args[0], args[1]);
            case OR: return or(ctx, args[0], args[1]);
            case XOR: return xor(ctx, args[0], args[1]);

            case EQUALS: return strictEquals(ctx, args[0], args[1]);
            case NOT_EQUALS: return !strictEquals(ctx, args[0], args[1]);
            case LOOSE_EQUALS: return looseEqual(ctx, args[0], args[1]);
            case LOOSE_NOT_EQUALS: return !looseEqual(ctx, args[0], args[1]);

            case GREATER: return compare(ctx, args[0], args[1]) > 0;
            case GREATER_EQUALS: return compare(ctx, args[0], args[1]) >= 0;
            case LESS: return compare(ctx, args[0], args[1]) < 0;
            case LESS_EQUALS: return compare(ctx, args[0], args[1]) <= 0;

            case INVERSE: return bitwiseNot(ctx, args[0]);
            case NOT: return not(args[0]);
            case POS: return toNumber(ctx, args[0]);
            case NEG: return negative(ctx, args[0]);

            case SHIFT_LEFT: return shiftLeft(ctx, args[0], args[1]);
            case SHIFT_RIGHT: return shiftRight(ctx, args[0], args[1]);
            case USHIFT_RIGHT: return unsignedShiftRight(ctx, args[0], args[1]);

            case IN: return hasMember(ctx, args[1], args[0], false);
            case INSTANCEOF: {
                var proto = getMember(ctx, args[1], "prototype");
                return isInstanceOf(ctx, args[0], proto);
            }

            default: return null;
        }
    }

    public static Object getMember(Context ctx, Object obj, Object key) throws InterruptedException {
        obj = normalize(ctx, obj); key = normalize(ctx, key);
        if (obj == null) throw new IllegalArgumentException("Tried to access member of undefined.");
        if (obj == NULL) throw new IllegalArgumentException("Tried to access member of null.");
        if (isObject(obj)) return object(obj).getMember(ctx, key);

        if (obj instanceof String && key instanceof Number) {
            var i = number(key);
            var s = (String)obj;
            if (i >= 0 && i < s.length() && i - Math.floor(i) == 0) {
                return s.charAt((int)i) + "";
            }
        }

        var proto = getPrototype(ctx, obj);

        if (proto == null) return key.equals("__proto__") ? NULL : null;
        else if (key != null && key.equals("__proto__")) return proto;
        else return proto.getMember(ctx, key, obj);
    }
    public static boolean setMember(Context ctx, Object obj, Object key, Object val) throws InterruptedException {
        obj = normalize(ctx, obj); key = normalize(ctx, key); val = normalize(ctx, val);
        if (obj == null) throw EngineException.ofType("Tried to access member of undefined.");
        if (obj == NULL) throw EngineException.ofType("Tried to access member of null.");
        if (key.equals("__proto__")) return setPrototype(ctx, obj, val);
        if (isObject(obj)) return object(obj).setMember(ctx, key, val, false);

        var proto = getPrototype(ctx, obj);
        return proto.setMember(ctx, key, val, obj, true);
    }
    public static boolean hasMember(Context ctx, Object obj, Object key, boolean own) throws InterruptedException {
        if (obj == null || obj == NULL) return false;
        obj = normalize(ctx, obj); key = normalize(ctx, key);

        if (key.equals("__proto__")) return true;
        if (isObject(obj)) return object(obj).hasMember(ctx, key, own);

        if (obj instanceof String && key instanceof Number) {
            var i = number(key);
            var s = (String)obj;
            if (i >= 0 && i < s.length() && i - Math.floor(i) == 0) return true;
        }

        if (own) return false;

        var proto = getPrototype(ctx, obj);
        return proto != null && proto.hasMember(ctx, key, own);
    }
    public static boolean deleteMember(Context ctx, Object obj, Object key) throws InterruptedException {
        if (obj == null || obj == NULL) return false;
        obj = normalize(ctx, obj); key = normalize(ctx, key);

        if (isObject(obj)) return object(obj).deleteMember(ctx, key);
        else return false;
    }
    public static ObjectValue getPrototype(Context ctx, Object obj) throws InterruptedException {
        if (obj == null || obj == NULL) return null;
        obj = normalize(ctx, obj);
        if (isObject(obj)) return object(obj).getPrototype(ctx);
        if (ctx == null) return null;

        if (obj instanceof String) return ctx.function.proto("string");
        else if (obj instanceof Number) return ctx.function.proto("number");
        else if (obj instanceof Boolean) return ctx.function.proto("bool");
        else if (obj instanceof Symbol) return ctx.function.proto("symbol");

        return null;
    }
    public static boolean setPrototype(Context ctx, Object obj, Object proto) throws InterruptedException {
        obj = normalize(ctx, obj);
        return isObject(obj) && object(obj).setPrototype(ctx, proto);
    }
    public static List<Object> getMembers(Context ctx, Object obj, boolean own, boolean includeNonEnumerable) throws InterruptedException {  
        List<Object> res = new ArrayList<>();

        if (isObject(obj)) res = object(obj).keys(includeNonEnumerable);
        if (obj instanceof String) {
            for (var i = 0; i < ((String)obj).length(); i++) res.add((double)i);
        }

        if (!own) {
            var proto = getPrototype(ctx, obj);

            while (proto != null) {
                res.addAll(proto.keys(includeNonEnumerable));
                proto = proto.getPrototype(ctx);
            }
        }


        return res;
    }

    public static Object call(Context ctx, Object func, Object thisArg, Object ...args) throws InterruptedException {
        if (!isFunction(func))
            throw EngineException.ofType("Tried to call a non-function value.");
        return function(func).call(ctx, thisArg, args);
    }

    public static boolean strictEquals(Context ctx, Object a, Object b) {
        a = normalize(ctx, a); b = normalize(ctx, b);

        if (a == null || b == null) return a == null && b == null;
        if (isNan(a) || isNan(b)) return false;
        if (a instanceof Number && number(a) == -0.) a = 0.;
        if (b instanceof Number && number(b) == -0.) b = 0.;

        return a == b || a.equals(b);
    }
    public static boolean looseEqual(Context ctx, Object a, Object b) throws InterruptedException {
        a = normalize(ctx, a); b = normalize(ctx, b);

        // In loose equality, null is equivalent to undefined
        if (a == NULL) a = null;
        if (b == NULL) b = null;

        if (a == null || b == null) return a == null && b == null;
        // If both are objects, just compare their references
        if (!isPrimitive(a) && !isPrimitive(b)) return a == b;

        // Convert values to primitives
        a = toPrimitive(ctx, a, ConvertHint.VALUEOF);
        b = toPrimitive(ctx, b, ConvertHint.VALUEOF);

        // Compare symbols by reference
        if (a instanceof Symbol || b instanceof Symbol) return a == b;
        if (a instanceof Boolean || b instanceof Boolean) return toBoolean(a) == toBoolean(b);
        if (a instanceof Number || b instanceof Number) return strictEquals(ctx, toNumber(ctx, a), toNumber(ctx, b));

        // Default to strings
        return toString(ctx, a).equals(toString(ctx, b));
    }

    public static Object normalize(Context ctx, Object val) {
        if (val instanceof Number) return number(val);
        if (isPrimitive(val) || val instanceof ObjectValue) return val;
        if (val instanceof Character) return val + "";

        if (val instanceof Map) {
            var res = new ObjectValue();

            for (var entry : ((Map<?, ?>)val).entrySet()) {
                res.defineProperty(ctx, entry.getKey(), entry.getValue());
            }

            return res;
        }

        if (val instanceof Iterable) {
            var res = new ArrayValue();

            for (var entry : ((Iterable<?>)val)) {
                res.set(ctx, res.size(), entry);
            }

            return res;
        }

        if (val instanceof Class) {
            if (ctx == null) return null;
            else return ctx.function.wrappersProvider.getConstr((Class<?>)val);
        }

        return new NativeWrapper(val);
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(Context ctx, Object obj, Class<T> clazz) throws InterruptedException {
        if (clazz == Void.class) return null;
        if (clazz == null || clazz == Object.class) return (T)obj;

        var err = new IllegalArgumentException(String.format("Cannot convert '%s' to '%s'.", type(obj), clazz.getName()));

        if (obj instanceof NativeWrapper) {
            var res = ((NativeWrapper)obj).wrapped;
            if (clazz.isInstance(res)) return (T)res;
        }

        if (obj instanceof ArrayValue) {
            
            if (clazz.isAssignableFrom(ArrayList.class)) {
                var raw = array(obj).toArray();
                var res = new ArrayList<>();
                for (var i = 0; i < raw.length; i++) res.add(convert(ctx, raw[i], Object.class));
                return (T)new ArrayList<>(res);
            }
            if (clazz.isAssignableFrom(HashSet.class)) {
                var raw = array(obj).toArray();
                var res = new HashSet<>();
                for (var i = 0; i < raw.length; i++) res.add(convert(ctx, raw[i], Object.class));
                return (T)new HashSet<>(res);
            }
            if (clazz.isArray()) {
                var raw = array(obj).toArray();
                Object res = Array.newInstance(clazz.getComponentType(), raw.length);
                for (var i = 0; i < raw.length; i++) Array.set(res, i, convert(ctx, raw[i], Object.class));
                return (T)res;
            }
        }

        if (obj instanceof ObjectValue && clazz.isAssignableFrom(HashMap.class)) {
            var res = new HashMap<>();
            for (var el : object(obj).values.entrySet()) res.put(
                convert(ctx, el.getKey(), null),
                convert(ctx, el.getValue(), null)
            );
            return (T)res;
        }

        if (clazz == String.class) return (T)toString(ctx, obj);
        if (clazz == Boolean.class || clazz == Boolean.TYPE) return (T)(Boolean)toBoolean(obj);
        if (clazz == Byte.class || clazz == byte.class) return (T)(Byte)(byte)toNumber(ctx, obj);
        if (clazz == Integer.class || clazz == int.class) return (T)(Integer)(int)toNumber(ctx, obj);
        if (clazz == Long.class || clazz == long.class) return (T)(Long)(long)toNumber(ctx, obj);
        if (clazz == Short.class || clazz == short.class) return (T)(Short)(short)toNumber(ctx, obj);
        if (clazz == Float.class || clazz == float.class) return (T)(Float)(float)toNumber(ctx, obj);
        if (clazz == Double.class || clazz == double.class) return (T)(Double)toNumber(ctx, obj);

        if (clazz == Character.class || clazz == char.class) {
            if (obj instanceof Number) return (T)(Character)(char)number(obj);
            else if (obj == NULL) throw new IllegalArgumentException("Cannot convert null to character.");
            else if (obj == null) throw new IllegalArgumentException("Cannot convert undefined to character.");
            else {
                var res = toString(ctx, obj);
                if (res.length() == 0) throw new IllegalArgumentException("Cannot convert empty string to character.");
                else return (T)(Character)res.charAt(0);
            }
        }

        if (obj == null) return null;
        if (clazz.isInstance(obj)) return (T)obj;

        throw err;
    }

    public static Iterable<Object> toJavaIterable(Context ctx, Object obj) throws InterruptedException {
        return () -> {
            try {
                var constr = getMember(ctx, ctx.function.proto("symbol"), "constructor");
                var symbol = getMember(ctx, constr, "iterator");

                var iteratorFunc = getMember(ctx, obj, symbol);
                if (!isFunction(iteratorFunc)) return Collections.emptyIterator();
                var iterator = getMember(ctx, call(ctx, iteratorFunc, obj), "next");
                if (!isFunction(iterator)) return Collections.emptyIterator();
                var iterable = obj;

                return new Iterator<Object>() {
                    private Object value = null;
                    public boolean consumed = true;
                    private FunctionValue next = function(iterator);

                    private void loadNext() throws InterruptedException {
                        if (next == null) value = null;
                        else if (consumed) {
                            var curr = object(next.call(ctx, iterable));
                            if (curr == null) { next = null; value = null; }
                            if (toBoolean(curr.getMember(ctx, "done"))) { next = null; value = null; }
                            else {
                                this.value = curr.getMember(ctx, "value");
                                consumed = false;
                            }
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        try {
                            loadNext();
                            return next != null;
                        }
                        catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    }
                    @Override
                    public Object next() {
                        try {
                            loadNext();
                            var res = value;
                            value = null;
                            consumed = true;
                            return res;
                        }
                        catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return null;
                        }
                    }
                };
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            catch (IllegalArgumentException | NullPointerException e) {
                return Collections.emptyIterator();
            }
        };
    }

    public static ObjectValue fromJavaIterable(Context ctx, Iterable<?> iterable) throws InterruptedException {
        var res = new ObjectValue();
        var it = iterable.iterator();

        try {
            var key = getMember(ctx, getMember(ctx, ctx.function.proto("symbol"), "constructor"), "iterator");
            res.defineProperty(ctx, key, new NativeFunction("", (_ctx, thisArg, args) -> thisArg));
        }
        catch (IllegalArgumentException | NullPointerException e) { }

        res.defineProperty(ctx, "next", new NativeFunction("", (_ctx, _th, _args) -> {
            if (!it.hasNext()) return new ObjectValue(ctx, Map.of("done", true));
            else return new ObjectValue(ctx, Map.of("value", it.next()));
        }));

        return res;
    }

    private static void printValue(Context ctx, Object val, HashSet<Object> passed, int tab) throws InterruptedException {
        if (passed.contains(val)) {
            System.out.print("[circular]");
            return;
        }

        var printed = true;

        if (val instanceof FunctionValue) {
            System.out.print("function ");
            var name = Values.getMember(ctx, val, "name");
            if (name != null) System.out.print(Values.toString(ctx, name));
            System.out.print("(...)");
            var loc = val instanceof CodeFunction ? ((CodeFunction)val).loc() : null;

            if (loc != null) System.out.print(" @ " + loc);
        }
        else if (val instanceof ArrayValue) {
            System.out.print("[");
            var obj = ((ArrayValue)val);
            for (int i = 0; i < obj.size(); i++) {
                if (i != 0) System.out.print(", ");
                else System.out.print(" ");
                if (obj.has(i)) printValue(ctx, obj.get(i), passed, tab);
                else System.out.print(", ");
            }
            System.out.print(" ] ");
        }
        else if (val instanceof NativeWrapper) {
            var obj = ((NativeWrapper)val).wrapped;
            System.out.print("Native " + obj.toString() + " ");
        }
        else printed = false;

        if (val instanceof ObjectValue) {
            if (tab > 3) {
                System.out.print("{...}");
                return;
            }
            passed.add(val);

            var obj = (ObjectValue)val;
            if (obj.values.size() + obj.properties.size() == 0) {
                if (!printed) System.out.println("{}");
            }
            else {
                System.out.println("{");

                for (var el : obj.values.entrySet()) {
                    for (int i = 0; i < tab + 1; i++) System.out.print("    ");
                    printValue(ctx, el.getKey(), passed, tab + 1);
                    System.out.print(": ");
                    printValue(ctx, el.getValue(), passed, tab + 1);
                    System.out.println(",");
                }
                for (var el : obj.properties.entrySet()) {
                    for (int i = 0; i < tab + 1; i++) System.out.print("    ");
                    printValue(ctx, el.getKey(), passed, tab + 1);
                    System.out.println(": [prop],");
                }
    
                for (int i = 0; i < tab; i++) System.out.print("    ");
                System.out.print("}");
    
                passed.remove(val);
            }
        }
        else if (val == null) System.out.print("undefined");
        else if (val == Values.NULL) System.out.print("null");
        else if (val instanceof String) System.out.print("'" + val + "'");
        else System.out.print(Values.toString(ctx, val));
    }
    public static void printValue(Context ctx, Object val) throws InterruptedException {
        printValue(ctx, val, new HashSet<>(), 0);
    }
}
