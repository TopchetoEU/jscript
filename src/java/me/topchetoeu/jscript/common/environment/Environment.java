package me.topchetoeu.jscript.common.environment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

public class Environment {
    public final Environment parent;
    private final Map<Key<Object>, Object> map = new HashMap<>();
    private final Set<Key<Object>> hidden = new HashSet<>();

    private final Map<MultiKey<Object>, Set<Object>> multi = new HashMap<>();
    private final Map<MultiKey<Object>, Set<Object>> multiHidden = new HashMap<>();

    @SuppressWarnings("unchecked")
    private <T> Set<T> getAll(MultiKey<T> key, boolean forceClone) {
        Set<T> parent = null, child = null;
        boolean cloned = false;

        if (this.parent != null && !hidden.contains(key)) {
            parent = this.parent.getAll(key, false);
            if (parent.size() == 0) parent = null;
            else if (multiHidden.containsKey(key)) {
                parent = new HashSet<>(parent);
                parent.removeAll(multiHidden.get(key));
                cloned = true;
            }
        }
        if (multi.containsKey(key)) {
            child = (Set<T>)multi.get(key);
            if (child.size() == 0) child = null;
        }

        if (!forceClone) {
            if (parent == null && child == null) return Set.of();
            if (parent == null && child != null) return child;
            if (parent != null && child == null) return parent;
        }

        if (!cloned) parent = new HashSet<>();
        parent.addAll(child);
        return parent;
    }
    private <T> T getMulti(MultiKey<T> key) {
        return key.of(getAll(key, false));
    }
    private boolean hasMulti(MultiKey<?> key) {
        return getAll(key, false).size() > 0;
    }

    @SuppressWarnings("all")
    private <T> Environment addMulti(MultiKey<T> key, T value) {
        if (!multi.containsKey(key)) {
            if (hidden.contains(key)) {
                multiHidden.put((MultiKey)key, (Set)parent.getAll(key, true));
                hidden.remove(key);
            }

            multi.put((MultiKey)key, new HashSet<>());
        }

        multi.get(key).add(value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Key<T> key) {
        if (key instanceof MultiKey) return getMulti((MultiKey<T>)key);

        if (map.containsKey(key)) return (T)map.get(key);
        else if (!hidden.contains(key) && parent != null) return parent.get(key);
        else return null;
    }
    public boolean has(Key<?> key) {
        if (key instanceof MultiKey) return hasMulti((MultiKey<?>)key);

        if (map.containsKey(key)) return true;
        else if (!hidden.contains(key) && parent != null) return parent.has(key);
        else return false;
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
        if (key instanceof MultiKey) return add(key, val);

        map.put((Key<Object>)key, val);
        hidden.remove(key);
        return this;
    }
    public Environment add(Key<Void> key) {
        return add(key, null);
    }
    @SuppressWarnings("all")
    public Environment addAll(Map<Key<?>, ?> map, boolean iterableAsMulti) {
        for (var pair : map.entrySet()) {
            if (iterableAsMulti && pair.getKey() instanceof MultiKey && pair.getValue() instanceof Iterable) {
                for (var val : (Iterable<?>)pair.getValue()) {
                    addMulti((MultiKey<Object>)pair.getKey(), val);
                }
            }
            else add((Key<Object>)pair.getKey(), pair.getValue());
        }
        map.putAll((Map)map);
        hidden.removeAll(map.keySet());
        return this;
    }
    public Environment addAll(Map<Key<?>, ?> map) {
        return addAll(map, true);
    }
    // public Environment addAll(Environment env) {
    //     this.map.putAll(env.map);
    //     this.hidden.removeAll(env.map.keySet());

    //     for (var el : env.multi.entrySet()) {
    //         for (var val : el.getValue()) {
    //             add(el.getKey(), val);
    //         }
    //     }

    //     return this;
    // }

    @SuppressWarnings("unchecked")
    public Environment remove(Key<?> key) {
        map.remove(key);
        multi.remove(key);
        multiHidden.remove(key);
        hidden.add((Key<Object>)key);
        return this;
    }
    @SuppressWarnings("all")
    public <T> Environment remove(MultiKey<T> key, T val) {
        if (multi.containsKey(key)) {
            multi.get(key).remove(val);
            multiHidden.get(key).add(val);

            if (multi.get(key).size() == 0) {
                multi.remove(key);
                multiHidden.remove(key);
                hidden.add((Key)key);
            }
        }

        return this;
    }

    public <T> T init(Key<T> key, T val) {
        if (!has(key)) this.add(key, val);
        return val;
    }
    public <T> T initFrom(Key<T> key, Supplier<T> val) {
        if (!has(key)) {
            var res = val.get();
            this.add(key, res);
            return res;
        }
        else return get(key);
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

    public static Environment wrap(Environment env) {
        if (env == null) return empty();
        else return env;
    }

    public static Environment empty() {
        return new Environment();
    }

    public static int nextId() {
        return new Random().nextInt();
    }
}
