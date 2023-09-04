package me.topchetoeu.jscript.engine;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

@SuppressWarnings("unchecked")
public class CallContext {
    public static final class DataKey<T> {}

    public final Engine engine;
    public Environment environment;
    private final Map<DataKey<?>, Object> data;

    public Map<DataKey<?>, Object> data() { return Collections.unmodifiableMap(data); }

    public CallContext copy() {
        return new CallContext(engine, environment).mergeData(data);
    }

    public CallContext mergeData(Map<DataKey<?>, Object> objs) {
        data.putAll(objs);
        return this;
    }
    public <T> CallContext setData(DataKey<T> key, T val) {
        if (val == null) data.remove(key);
        else data.put(key, val);
        return this;
    }
    public <T> T addData(DataKey<T> key, T val) {
        if (data.containsKey(key)) return (T)data.get(key);
        else {
            if (val == null) data.remove(key);
            else data.put(key, val);
            return val;
        }
    }
    public <T> T getData(DataKey<T> key) {
        return getData(key, null);
    }
    public <T> T getData(DataKey<T> key, T defaultVal) {
        if (!hasData(key)) return defaultVal;
        else return (T)data.get(key);
    }
    public boolean hasData(DataKey<?> key) { return data.containsKey(key); }

    public CallContext changeData(DataKey<Integer> key, int n, int start) {
        return setData(key, getData(key, start) + n);
    }
    public CallContext changeData(DataKey<Integer> key, int n) {
        return changeData(key, n, 0);
    }
    public CallContext changeData(DataKey<Integer> key) {
        return changeData(key, 1, 0);
    }

    public CallContext(Engine engine, Environment env) {
        this.engine = engine;
        this.data = new Hashtable<>();
        this.environment = env;
    }
    public CallContext(CallContext parent, Environment env) {
        this.engine = parent.engine;
        this.data = parent.data;
        this.environment = env;
    }
}
