package me.topchetoeu.jscript.core;

public interface Extensions {
    <T> T get(Key<T> key);
    <T> void add(Key<T> key, T obj);
    Iterable<Key<?>> keys();

    boolean has(Key<?> key);
    boolean remove(Key<?> key);

    default void add(Key<Void> key) {
        add(key, null);
    }

    default boolean hasNotNull(Key<?> key) {
        return has(key) && get(key) != null;
    }

    default <T> T get(Key<T> key, T defaultVal) {
        if (has(key)) return get(key);
        else return defaultVal;
    }

    default <T> T init(Key<T> key, T val) {
        if (has(key)) return get(key);
        else {
            add(key, val);
            return val;
        }
    }
    @SuppressWarnings("unchecked")
    default void addAll(Extensions source) {
        if (source == null) return;
        for (var key : source.keys()) {
            add((Key<Object>)key, (Object)source.get(key));
        }
    }
}
