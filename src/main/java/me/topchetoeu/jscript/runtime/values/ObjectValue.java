package me.topchetoeu.jscript.runtime.values;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import me.topchetoeu.jscript.runtime.Environment;
import me.topchetoeu.jscript.runtime.Extensions;

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

    private Property getProperty(Extensions ext, Object key) {
        if (properties.containsKey(key)) return properties.get(key);
        var proto = getPrototype(ext);
        if (proto != null) return proto.getProperty(ext, key);
        else return null;
    }

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

    public final boolean defineProperty(Extensions ext, Object key, Object val, boolean writable, boolean configurable, boolean enumerable) {
        key = Values.normalize(ext, key); val = Values.normalize(ext, val);
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
    public final boolean defineProperty(Extensions ext, Object key, Object val) {
        return defineProperty(ext, key, val, true, true, true);
    }
    public final boolean defineProperty(Extensions ext, Object key, FunctionValue getter, FunctionValue setter, boolean configurable, boolean enumerable) {
        key = Values.normalize(ext, key);
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

    public ObjectValue getPrototype(Extensions ext) {
        if (prototype instanceof ObjectValue || prototype == null) return (ObjectValue)prototype;

        try {
            if (prototype == ARR_PROTO) return ext.get(Environment.ARRAY_PROTO);
            if (prototype == FUNC_PROTO) return ext.get(Environment.FUNCTION_PROTO);
            if (prototype == ERR_PROTO) return ext.get(Environment.ERROR_PROTO);
            if (prototype == RANGE_ERR_PROTO) return ext.get(Environment.RANGE_ERR_PROTO);
            if (prototype == SYNTAX_ERR_PROTO) return ext.get(Environment.SYNTAX_ERR_PROTO);
            if (prototype == TYPE_ERR_PROTO) return ext.get(Environment.TYPE_ERR_PROTO);
            return ext.get(Environment.OBJECT_PROTO);
        }
        catch (NullPointerException e) { return null; }
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

    /**
     * A method, used to get the value of a field. If a property is bound to
     * this key, but not a field, this method should return null.
     */
    protected Object getField(Extensions ext, Object key) {
        if (values.containsKey(key)) return values.get(key);
        var proto = getPrototype(ext);
        if (proto != null) return proto.getField(ext, key);
        else return null;
    }
    /**
     * Changes the value of a field, that is bound to the given key. If no field is
     * bound to this key, a new field should be created with the given value
     * @return Whether or not the operation was successful
     */
    protected boolean setField(Extensions ext, Object key, Object val) {
        if (val instanceof FunctionValue && ((FunctionValue)val).name.equals("")) {
            ((FunctionValue)val).name = Values.toString(ext, key);
        }

        values.put(key, val);
        return true;
    }
    /**
     * Deletes the field bound to the given key.
     */
    protected void deleteField(Extensions ext, Object key) {
        values.remove(key);
    }
    /**
     * Returns whether or not there is a field bound to the given key.
     * This must ignore properties
     */
    protected boolean hasField(Extensions ext, Object key) {
        return values.containsKey(key);
    }

    public final Object getMember(Extensions ext, Object key, Object thisArg) {
        key = Values.normalize(ext, key);

        if ("__proto__".equals(key)) {
            var res = getPrototype(ext);
            return res == null ? Values.NULL : res;
        }

        var prop = getProperty(ext, key);

        if (prop != null) {
            if (prop.getter == null) return null;
            else return prop.getter.call(ext, Values.normalize(ext, thisArg));
        }
        else return getField(ext, key);
    }
    public final boolean setMember(Extensions ext, Object key, Object val, Object thisArg, boolean onlyProps) {
        key = Values.normalize(ext, key); val = Values.normalize(ext, val);

        var prop = getProperty(ext, key);
        if (prop != null) {
            if (prop.setter == null) return false;
            prop.setter.call(ext, Values.normalize(ext, thisArg), val);
            return true;
        }
        else if (onlyProps) return false;
        else if (!extensible() && !values.containsKey(key)) return false;
        else if (key == null) {
            values.put(key, val);
            return true;
        }
        else if ("__proto__".equals(key)) return setPrototype(ext, val);
        else if (nonWritableSet.contains(key)) return false;
        else return setField(ext, key, val);
    }
    public final boolean hasMember(Extensions ext, Object key, boolean own) {
        key = Values.normalize(ext, key);

        if (key != null && "__proto__".equals(key)) return true;
        if (hasField(ext, key)) return true;
        if (properties.containsKey(key)) return true;
        if (own) return false;
        var proto = getPrototype(ext);
        return proto != null && proto.hasMember(ext, key, own);
    }
    public final boolean deleteMember(Extensions ext, Object key) {
        key = Values.normalize(ext, key);

        if (!memberConfigurable(key)) return false;
        properties.remove(key);
        nonWritableSet.remove(key);
        nonEnumerableSet.remove(key);
        deleteField(ext, key);
        return true;
    }
    public final boolean setPrototype(Extensions ext, Object val) {
        val = Values.normalize(ext, val);

        if (!extensible()) return false;
        if (val == null || val == Values.NULL) {
            prototype = null;
            return true;
        }
        else if (val instanceof ObjectValue) {
            var obj = (ObjectValue)val;

            if (ext != null) {
                if (obj == ext.get(Environment.OBJECT_PROTO)) prototype = OBJ_PROTO;
                else if (obj == ext.get(Environment.ARRAY_PROTO)) prototype = ARR_PROTO;
                else if (obj == ext.get(Environment.FUNCTION_PROTO)) prototype = FUNC_PROTO;
                else if (obj == ext.get(Environment.ERROR_PROTO)) prototype = ERR_PROTO;
                else if (obj == ext.get(Environment.SYNTAX_ERR_PROTO)) prototype = SYNTAX_ERR_PROTO;
                else if (obj == ext.get(Environment.TYPE_ERR_PROTO)) prototype = TYPE_ERR_PROTO;
                else if (obj == ext.get(Environment.RANGE_ERR_PROTO)) prototype = RANGE_ERR_PROTO;
                else prototype = obj;
            }
            else prototype = obj;

            return true;
        }
        return false;
    }

    public final ObjectValue getMemberDescriptor(Extensions ext, Object key) {
        key = Values.normalize(ext, key);

        var prop = properties.get(key);
        var res = new ObjectValue();

        res.defineProperty(ext, "configurable", memberConfigurable(key));
        res.defineProperty(ext, "enumerable", memberEnumerable(key));

        if (prop != null) {
            res.defineProperty(ext, "get", prop.getter);
            res.defineProperty(ext, "set", prop.setter);
        }
        else if (hasField(ext, key)) {
            res.defineProperty(ext, "value", values.get(key));
            res.defineProperty(ext, "writable", memberWritable(key));
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

    public ObjectValue(Extensions ext, Map<?, ?> values) {
        this(PlaceholderProto.OBJECT);
        for (var el : values.entrySet()) {
            defineProperty(ext, el.getKey(), el.getValue());
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
