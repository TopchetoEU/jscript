package me.topchetoeu.jscript.engine.values;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import me.topchetoeu.jscript.engine.CallContext;

public class ObjectValue {
    public static enum PlaceholderProto {
        NONE,
        OBJECT,
        ARRAY,
        FUNCTION,
        ERROR,
        SYNTAX_ERROR,
        TYPE_ERROR,
        RANGE_ERROR,
    }
    public static enum State {
        NORMAL,
        NO_EXTENSIONS,
        SEALED,
        FROZEN,
    }

    public static class Property { 
        public final FunctionValue getter;
        public final FunctionValue setter;

        public Property(FunctionValue getter, FunctionValue setter) {
            this.getter = getter;
            this.setter = setter;
        }
    }

    private static final Object OBJ_PROTO = new Object();
    private static final Object ARR_PROTO = new Object();
    private static final Object FUNC_PROTO = new Object();
    private static final Object ERR_PROTO = new Object();
    private static final Object SYNTAX_ERR_PROTO = new Object();
    private static final Object TYPE_ERR_PROTO = new Object();
    private static final Object RANGE_ERR_PROTO = new Object();

    protected Object prototype;

    public State state = State.NORMAL;
    public HashMap<Object, Object> values = new HashMap<>();
    public HashMap<Object, Property> properties = new HashMap<>();
    public HashSet<Object> nonWritableSet = new HashSet<>();
    public HashSet<Object> nonConfigurableSet = new HashSet<>();
    public HashSet<Object> nonEnumerableSet = new HashSet<>();

    public final boolean memberWritable(Object key) {
        if (state == State.FROZEN) return false;
        return !nonWritableSet.contains(key);
    }
    public final boolean memberConfigurable(Object key) {
        if (state == State.SEALED || state == State.FROZEN) return false;
        return !nonConfigurableSet.contains(key);
    }
    public final boolean memberEnumerable(Object key) {
        return !nonEnumerableSet.contains(key);
    }
    public final boolean extensible() {
        return state == State.NORMAL;
    }

    public final void preventExtensions() {
        if (state == State.NORMAL) state = State.NO_EXTENSIONS;
    }
    public final void seal() {
        if (state == State.NORMAL || state == State.NO_EXTENSIONS) state = State.SEALED;
    }
    public final void freeze() {
        state = State.FROZEN;
    }

    public final boolean defineProperty(CallContext ctx, Object key, Object val, boolean writable, boolean configurable, boolean enumerable) {
        key = Values.normalize(ctx, key); val = Values.normalize(ctx, val);
        boolean reconfigured = 
            writable != memberWritable(key) ||
            configurable != memberConfigurable(key) ||
            enumerable != memberEnumerable(key);

        if (!reconfigured) {
            if (!memberWritable(key)) {
                var a = values.get(key);
                var b = val;
                if (a == null || b == null) return a == null && b == null;
                return a == b || a.equals(b);
            }
            values.put(key, val);
            return true;
        }

        if (
            properties.containsKey(key) &&
            values.get(key) == val &&
            !reconfigured
        ) return true;

        if (!extensible() && !values.containsKey(key) && !properties.containsKey(key)) return false;
        if (!memberConfigurable(key))
            return false;

        nonWritableSet.remove(key);
        nonEnumerableSet.remove(key);
        properties.remove(key);
        values.remove(key);

        if (!writable) nonWritableSet.add(key);
        if (!configurable) nonConfigurableSet.add(key);
        if (!enumerable) nonEnumerableSet.add(key);

        values.put(key, val);
        return true;
    }
    public final boolean defineProperty(CallContext ctx, Object key, Object val) {
        return defineProperty(ctx, key, val, true, true, true);
    }
    public final boolean defineProperty(CallContext ctx, Object key, FunctionValue getter, FunctionValue setter, boolean configurable, boolean enumerable) {
        key = Values.normalize(ctx, key);
        if (
            properties.containsKey(key) &&
            properties.get(key).getter == getter &&
            properties.get(key).setter == setter &&
            !configurable == nonConfigurableSet.contains(key) &&
            !enumerable == nonEnumerableSet.contains(key)
        ) return true;
        if (!extensible() && !values.containsKey(key) && !properties.containsKey(key)) return false;
        if (!memberConfigurable(key)) return false;

        nonWritableSet.remove(key);
        nonEnumerableSet.remove(key);
        properties.remove(key);
        values.remove(key);

        if (!configurable) nonConfigurableSet.add(key);
        if (!enumerable) nonEnumerableSet.add(key);

        properties.put(key, new Property(getter, setter));
        return true;
    }

    public ObjectValue getPrototype(CallContext ctx) throws InterruptedException {
        try {
            if (prototype == OBJ_PROTO) return ctx.engine().objectProto();
            if (prototype == ARR_PROTO) return ctx.engine().arrayProto();
            if (prototype == FUNC_PROTO) return ctx.engine().functionProto();
            if (prototype == ERR_PROTO) return ctx.engine().errorProto();
            if (prototype == RANGE_ERR_PROTO) return ctx.engine().rangeErrorProto();
            if (prototype == SYNTAX_ERR_PROTO) return ctx.engine().syntaxErrorProto();
            if (prototype == TYPE_ERR_PROTO) return ctx.engine().typeErrorProto();
        }
        catch (NullPointerException e) {
            return null;
        }

        return (ObjectValue)prototype;
    }
    public final boolean setPrototype(CallContext ctx, Object val) {
        val = Values.normalize(ctx, val);

        if (!extensible()) return false;
        if (val == null || val == Values.NULL) prototype = null;
        else if (Values.isObject(val)) {
            var obj = Values.object(val);

            if (ctx != null && ctx.engine() != null) {
                if (obj == ctx.engine().objectProto()) prototype = OBJ_PROTO;
                else if (obj == ctx.engine().arrayProto()) prototype = ARR_PROTO;
                else if (obj == ctx.engine().functionProto()) prototype = FUNC_PROTO;
                else if (obj == ctx.engine().errorProto()) prototype = ERR_PROTO;
                else if (obj == ctx.engine().syntaxErrorProto()) prototype = SYNTAX_ERR_PROTO;
                else if (obj == ctx.engine().typeErrorProto()) prototype = TYPE_ERR_PROTO;
                else if (obj == ctx.engine().rangeErrorProto()) prototype = RANGE_ERR_PROTO;
                else prototype = obj;
            }
            else prototype = obj;

            return true;
        }
        return false;
    }
    public final boolean setPrototype(PlaceholderProto val) {
        if (!extensible()) return false;
        switch (val) {
            case OBJECT: prototype = OBJ_PROTO; break;
            case FUNCTION: prototype = FUNC_PROTO; break;
            case ARRAY: prototype = ARR_PROTO; break;
            case ERROR: prototype = ERR_PROTO; break;
            case SYNTAX_ERROR: prototype = SYNTAX_ERR_PROTO; break;
            case TYPE_ERROR: prototype = TYPE_ERR_PROTO; break;
            case RANGE_ERROR: prototype = RANGE_ERR_PROTO; break;
            case NONE: prototype = null; break;
        }
        return true;
    }

    protected Property getProperty(CallContext ctx, Object key) throws InterruptedException {
        if (properties.containsKey(key)) return properties.get(key);
        var proto = getPrototype(ctx);
        if (proto != null) return proto.getProperty(ctx, key);
        else return null;
    }
    protected Object getField(CallContext ctx, Object key) throws InterruptedException {
        if (values.containsKey(key)) return values.get(key);
        var proto = getPrototype(ctx);
        if (proto != null) return proto.getField(ctx, key);
        else return null;
    }
    protected boolean setField(CallContext ctx, Object key, Object val) throws InterruptedException {
        if (val instanceof FunctionValue && ((FunctionValue)val).name.equals("")) {
            ((FunctionValue)val).name = Values.toString(ctx, key);
        }

        values.put(key, val);
        return true;
    }
    protected void deleteField(CallContext ctx, Object key) throws InterruptedException {
        values.remove(key);
    }
    protected boolean hasField(CallContext ctx, Object key) throws InterruptedException {
        return values.containsKey(key);
    }

    public final Object getMember(CallContext ctx, Object key, Object thisArg) throws InterruptedException {
        key = Values.normalize(ctx, key);

        if (key.equals("__proto__")) {
            var res = getPrototype(ctx);
            return res == null ? Values.NULL : res;
        }

        var prop = getProperty(ctx, key);

        if (prop != null) {
            if (prop.getter == null) return null;
            else return prop.getter.call(ctx, Values.normalize(ctx, thisArg));
        }
        else return getField(ctx, key);
    }
    public final Object getMember(CallContext ctx, Object key) throws InterruptedException {
        return getMember(ctx, key, this);
    }

    public final boolean setMember(CallContext ctx, Object key, Object val, Object thisArg, boolean onlyProps) throws InterruptedException {
        key = Values.normalize(ctx, key); val = Values.normalize(ctx, val);

        var prop = getProperty(ctx, key);
        if (prop != null) {
            if (prop.setter == null) return false;
            prop.setter.call(ctx, Values.normalize(ctx, thisArg), val);
            return true;
        }
        else if (onlyProps) return false;
        else if (!extensible() && !values.containsKey(key)) return false;
        else if (key == null) {
            values.put(key, val);
            return true;
        }
        else if (key.equals("__proto__")) return setPrototype(ctx, val);
        else if (nonWritableSet.contains(key)) return false;
        else return setField(ctx, key, val);
    }
    public final boolean setMember(CallContext ctx, Object key, Object val, boolean onlyProps) throws InterruptedException {
        return setMember(ctx, Values.normalize(ctx, key), Values.normalize(ctx, val), this, onlyProps);
    }

    public final boolean hasMember(CallContext ctx, Object key, boolean own) throws InterruptedException {
        key = Values.normalize(ctx, key);

        if (key != null && key.equals("__proto__")) return true;
        if (hasField(ctx, key)) return true;
        if (properties.containsKey(key)) return true;
        if (own) return false;
        return prototype != null && getPrototype(ctx).hasMember(ctx, key, own);
    }
    public final boolean deleteMember(CallContext ctx, Object key) throws InterruptedException {
        key = Values.normalize(ctx, key);

        if (!memberConfigurable(key)) return false;
        properties.remove(key);
        nonWritableSet.remove(key);
        nonEnumerableSet.remove(key);
        deleteField(ctx, key);
        return true;
    }

    public final ObjectValue getMemberDescriptor(CallContext ctx, Object key) throws InterruptedException {
        key = Values.normalize(ctx, key);

        var prop = properties.get(key);
        var res = new ObjectValue();

        res.defineProperty(ctx, "configurable", memberConfigurable(key));
        res.defineProperty(ctx, "enumerable", memberEnumerable(key));

        if (prop != null) {
            res.defineProperty(ctx, "get", prop.getter);
            res.defineProperty(ctx, "set", prop.setter);
        }
        else if (hasField(ctx, key)) {
            res.defineProperty(ctx, "value", values.get(key));
            res.defineProperty(ctx, "writable", memberWritable(key));
        }
        else return null;
        return res;
    }

    public List<Object> keys(boolean includeNonEnumerable) {
        var res = new ArrayList<Object>();

        for (var key : values.keySet()) {
            if (nonEnumerableSet.contains(key) && !includeNonEnumerable) continue;
            res.add(key);
        }
        for (var key : properties.keySet()) {
            if (nonEnumerableSet.contains(key) && !includeNonEnumerable) continue;
            res.add(key);
        }

        return res;
    }

    public ObjectValue(CallContext ctx, Map<?, ?> values) {
        this(PlaceholderProto.OBJECT);
        for (var el : values.entrySet()) {
            defineProperty(ctx, el.getKey(), el.getValue());
        }
    }
    public ObjectValue(PlaceholderProto proto) {
        nonConfigurableSet.add("__proto__");
        nonEnumerableSet.add("__proto__");
        setPrototype(proto);
    }
    public ObjectValue() {
        this(PlaceholderProto.OBJECT);
    }
}
