package me.topchetoeu.jscript.engine.values;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import me.topchetoeu.jscript.engine.Context;

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
    public LinkedHashMap<Object, Object> values = new LinkedHashMap<>();
    public LinkedHashMap<Object, Property> properties = new LinkedHashMap<>();
    public LinkedHashSet<Object> nonWritableSet = new LinkedHashSet<>();
    public LinkedHashSet<Object> nonConfigurableSet = new LinkedHashSet<>();
    public LinkedHashSet<Object> nonEnumerableSet = new LinkedHashSet<>();

    public final boolean memberWritable(Object key) {
        if (state == State.FROZEN) return false;
        return !values.containsKey(key) || !nonWritableSet.contains(key);
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

    public final boolean defineProperty(Context ctx, Object key, Object val, boolean writable, boolean configurable, boolean enumerable) {
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
        if (!memberConfigurable(key)) return false;

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
    public final boolean defineProperty(Context ctx, Object key, Object val) {
        return defineProperty(ctx, key, val, true, true, true);
    }
    public final boolean defineProperty(Context ctx, Object key, FunctionValue getter, FunctionValue setter, boolean configurable, boolean enumerable) {
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

    public ObjectValue getPrototype(Context ctx) {
        try {
            if (prototype == OBJ_PROTO) return ctx.environment().proto("object");
            if (prototype == ARR_PROTO) return ctx.environment().proto("array");
            if (prototype == FUNC_PROTO) return ctx.environment().proto("function");
            if (prototype == ERR_PROTO) return ctx.environment().proto("error");
            if (prototype == RANGE_ERR_PROTO) return ctx.environment().proto("rangeErr");
            if (prototype == SYNTAX_ERR_PROTO) return ctx.environment().proto("syntaxErr");
            if (prototype == TYPE_ERR_PROTO) return ctx.environment().proto("typeErr");
        }
        catch (NullPointerException e) { return null; }

        return (ObjectValue)prototype;
    }
    public final boolean setPrototype(Context ctx, Object val) {
        val = Values.normalize(ctx, val);

        if (!extensible()) return false;
        if (val == null || val == Values.NULL) {
            prototype = null;
            return true;
        }
        else if (Values.isObject(val)) {
            var obj = Values.object(val);

            if (ctx != null && ctx.environment() != null) {
                if (obj == ctx.environment().proto("object")) prototype = OBJ_PROTO;
                else if (obj == ctx.environment().proto("array")) prototype = ARR_PROTO;
                else if (obj == ctx.environment().proto("function")) prototype = FUNC_PROTO;
                else if (obj == ctx.environment().proto("error")) prototype = ERR_PROTO;
                else if (obj == ctx.environment().proto("syntaxErr")) prototype = SYNTAX_ERR_PROTO;
                else if (obj == ctx.environment().proto("typeErr")) prototype = TYPE_ERR_PROTO;
                else if (obj == ctx.environment().proto("rangeErr")) prototype = RANGE_ERR_PROTO;
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

    protected Property getProperty(Context ctx, Object key) {
        if (properties.containsKey(key)) return properties.get(key);
        var proto = getPrototype(ctx);
        if (proto != null) return proto.getProperty(ctx, key);
        else return null;
    }
    protected Object getField(Context ctx, Object key) {
        if (values.containsKey(key)) return values.get(key);
        var proto = getPrototype(ctx);
        if (proto != null) return proto.getField(ctx, key);
        else return null;
    }
    protected boolean setField(Context ctx, Object key, Object val) {
        if (val instanceof FunctionValue && ((FunctionValue)val).name.equals("")) {
            ((FunctionValue)val).name = Values.toString(ctx, key);
        }

        values.put(key, val);
        return true;
    }
    protected void deleteField(Context ctx, Object key) {
        values.remove(key);
    }
    protected boolean hasField(Context ctx, Object key) {
        return values.containsKey(key);
    }

    public final Object getMember(Context ctx, Object key, Object thisArg) {
        key = Values.normalize(ctx, key);

        if ("__proto__".equals(key)) {
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
    public final Object getMember(Context ctx, Object key) {
        return getMember(ctx, key, this);
    }

    public final boolean setMember(Context ctx, Object key, Object val, Object thisArg, boolean onlyProps) {
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
        else if ("__proto__".equals(key)) return setPrototype(ctx, val);
        else if (nonWritableSet.contains(key)) return false;
        else return setField(ctx, key, val);
    }
    public final boolean setMember(Context ctx, Object key, Object val, boolean onlyProps) {
        return setMember(ctx, Values.normalize(ctx, key), Values.normalize(ctx, val), this, onlyProps);
    }

    public final boolean hasMember(Context ctx, Object key, boolean own) {
        key = Values.normalize(ctx, key);

        if (key != null && "__proto__".equals(key)) return true;
        if (hasField(ctx, key)) return true;
        if (properties.containsKey(key)) return true;
        if (own) return false;
        var proto = getPrototype(ctx);
        return proto != null && proto.hasMember(ctx, key, own);
    }
    public final boolean deleteMember(Context ctx, Object key) {
        key = Values.normalize(ctx, key);

        if (!memberConfigurable(key)) return false;
        properties.remove(key);
        nonWritableSet.remove(key);
        nonEnumerableSet.remove(key);
        deleteField(ctx, key);
        return true;
    }

    public final ObjectValue getMemberDescriptor(Context ctx, Object key) {
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

    public ObjectValue(Context ctx, Map<?, ?> values) {
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
