package me.topchetoeu.jscript.runtime;

import java.util.List;

public interface Extensions {
    public static Extensions EMPTY = new Extensions() {
        @Override public <T> void add(Key<T> key, T obj) { }
        @Override public boolean remove(Key<?> key) { return false; }

        @Override public <T> T get(Key<T> key) { return null; }
        @Override public boolean has(Key<?> key) { return false; }
        @Override public Iterable<Key<?>> keys() { return List.of(); }
    };

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

    public static Extensions wrap(Extensions ext) {
        if (ext == null) return EMPTY;
        else return ext;
    }
}
