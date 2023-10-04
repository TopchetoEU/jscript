package me.topchetoeu.jscript.engine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

@SuppressWarnings("unchecked")
public class Data implements Iterable<Entry<DataKey<?>, ?>> {
    private HashMap<DataKey<Object>, Object> data = new HashMap<>();

    public Data copy() {
        return new Data().addAll(this);
    }

    public Data addAll(Iterable<Entry<DataKey<?>, ?>> data) {
        for (var el : data) {
            add((DataKey<Object>)el.getKey(), (Object)el.getValue());
        }
        return this;
    }

    public <T> Data set(DataKey<T> key, T val) {
        if (val == null) data.remove(key);
        else data.put((DataKey<Object>)key, (Object)val);
        return this;
    }
    public <T> T add(DataKey<T> key, T val) {
        if (data.containsKey(key)) return (T)data.get(key);
        else {
            if (val == null) data.remove(key);
            else data.put((DataKey<Object>)key, (Object)val);
            return val;
        }
    }
    public <T> T get(DataKey<T> key) {
        return get(key, null);
    }
    public <T> T get(DataKey<T> key, T defaultVal) {
        if (!has(key)) return defaultVal;
        else return (T)data.get(key);
    }
    public boolean has(DataKey<?> key) { return data.containsKey(key); }

    public int increase(DataKey<Integer> key, int n, int start) {
        int res;
        set(key, res = get(key, start) + n);
        return res;
    }
    public int increase(DataKey<Integer> key, int n) {
        return increase(key, n, 0);
    }
    public int increase(DataKey<Integer> key) {
        return increase(key, 1, 0);
    }

    @Override
    public Iterator<Entry<DataKey<?>, ?>> iterator() {
        return (Iterator<Entry<DataKey<?>, ?>>)data.entrySet();
    }
}
