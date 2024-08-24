package me.topchetoeu.jscript.runtime.environment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import me.topchetoeu.jscript.runtime.Compiler;
import me.topchetoeu.jscript.runtime.values.FunctionValue;
import me.topchetoeu.jscript.runtime.values.ObjectValue;

public class Environment {
    public static final Key<Compiler> COMPILE_FUNC = new Key<>();

    public static final Key<FunctionValue> REGEX_CONSTR = new Key<>();
    public static final Key<Integer> MAX_STACK_COUNT = new Key<>();
    public static final Key<Boolean> HIDE_STACK = new Key<>();

    public static final Key<ObjectValue> OBJECT_PROTO = new Key<>();
    public static final Key<ObjectValue> FUNCTION_PROTO = new Key<>();
    public static final Key<ObjectValue> ARRAY_PROTO = new Key<>();
    public static final Key<ObjectValue> BOOL_PROTO = new Key<>();
    public static final Key<ObjectValue> NUMBER_PROTO = new Key<>();
    public static final Key<ObjectValue> STRING_PROTO = new Key<>();
    public static final Key<ObjectValue> SYMBOL_PROTO = new Key<>();
    public static final Key<ObjectValue> ERROR_PROTO = new Key<>();
    public static final Key<ObjectValue> SYNTAX_ERR_PROTO = new Key<>();
    public static final Key<ObjectValue> TYPE_ERR_PROTO = new Key<>();
    public static final Key<ObjectValue> RANGE_ERR_PROTO = new Key<>();

    public final Environment parent;
    private final Map<Key<Object>, Object> map = new HashMap<>();
    private final Set<Key<Object>> hidden = new HashSet<>();

    @SuppressWarnings("unchecked")
    public <T> T get(Key<T> key) {
        if (map.containsKey(key)) return (T)map.get(key);
        else if (!hidden.contains(key) && parent != null) return parent.get(key);
        else return null;
    }
    public boolean has(Key<?> key) {
        if (map.containsKey(key)) return true;
        else if (!hidden.contains(key) && parent != null) return parent.has(key);
        else return false;
    }

    @SuppressWarnings("all")
    public Set<Key<?>> keys() {
        if (parent != null) {
            if (map.size() == 0) return (Set)map.keySet();

            var res = new HashSet();
            res.addAll(parent.keys());
            res.addAll(map.keySet());
            return res;
        }
        else return (Set)map.keySet();
    }

    public boolean hasNotNull(Key<?> key) {
        return get(key) != null;
    }

    public <T> T get(Key<T> key, T defaultVal) {
        if (has(key)) return get(key);
        else return defaultVal;
    }
    public <T> T get(Key<T> key, Supplier<T> defaultVal) {
        if (has(key)) return get(key);
        else return defaultVal.get();
    }

    @SuppressWarnings("unchecked")
    public <T> Environment add(Key<T> key, T val) {
        map.put((Key<Object>)key, val);
        hidden.remove(key);
        return this;
    }
    @SuppressWarnings("unchecked")
    public Environment add(Key<Void> key) {
        map.put((Key<Object>)(Key<?>)key, null);
        hidden.remove(key);
        return this;
    }
    @SuppressWarnings("all")
    public Environment addAll(Map<Key<?>, ?> map) {
        map.putAll((Map)map);
        hidden.removeAll(map.keySet());
        return this;
    }
    public Environment addAll(Environment env) {
        this.map.putAll(env.map);
        this.hidden.removeAll(env.map.keySet());
        return this;
    }

    @SuppressWarnings("unchecked")
    public Environment remove(Key<?> key) {
        map.remove((Key<Object>)key);
        hidden.add((Key<Object>)key);
        return this;
    }

    public <T> Environment init(Key<T> key, T val) {
        if (!has(key)) this.add(key, val);
        return this;
    }
    public <T> Environment init(Key<T> key, Supplier<T> val) {
        if (!has(key)) this.add(key, val.get());
        return this;
    }

    public Environment child() {
        return new Environment(this);
    }

    public Environment(Environment parent) {
        this.parent = parent;
    }
    public Environment() {
        this.parent = null;
    }

    public static Environment wrap(Environment ext) {
        if (ext == null) return empty();
        else return ext;
    }

    // public static Environment chain(int id, Environment ...envs) {
    //     var res = new Environment();
    //     for (var env : envs) res.addAll(env);
    //     return res;
    // }

    public static Environment empty() {
        return new Environment();
    }

    public static int nextId() {
        return new Random().nextInt();
    }
}
