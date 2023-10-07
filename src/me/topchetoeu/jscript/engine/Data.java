package me.topchetoeu.jscript.engine;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Data {
    public final Data parent;
    private HashMap<DataKey<Object>, Object> data = new HashMap<>();

    public Data copy() {
        return new Data().addAll(this);
    }

    public Data addAll(Map<DataKey<?>, ?> data) {
        for (var el : data.entrySet()) {
            get((DataKey<Object>)el.getKey(), (Object)el.getValue());
        }
        return this;
    }
    public Data addAll(Data data) {
        for (var el : data.data.entrySet()) {
            get((DataKey<Object>)el.getKey(), (Object)el.getValue());
        }
        return this;
    }

    public <T> T remove(DataKey<T> key) {
        return (T)data.remove(key);
    }
    public <T> Data set(DataKey<T> key, T val) {
        data.put((DataKey<Object>)key, (Object)val);
        return this;
    }
    public <T> T get(DataKey<T> key, T val) {
        for (var it = this; it != null; it = it.parent) {
            if (it.data.containsKey(key)) {
                this.set(key, val);
                return (T)data.get((DataKey<Object>)key);
            }
        }

        set(key, val);
        return val;
    }
    public <T> T get(DataKey<T> key) {
        for (var it = this; it != null; it = it.parent) {
            if (it.data.containsKey(key)) return (T)data.get((DataKey<Object>)key);
        }
        return null;
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

    public Data() {
        this.parent = null;
    }
    public Data(Data parent) {
        this.parent = parent;
    }
}
