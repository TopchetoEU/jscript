package me.topchetoeu.jscript.common.json;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JSONMap implements Map<String, JSONElement> {
    private Map<String, JSONElement> elements = new HashMap<>();

    public JSONElement get(String path) {
        var curr = this;
        var segs = path.split("\\.");
        var i = 0;

        while (true) {
            var tmp = curr.elements.get(segs[i++]);
            if (i == segs.length) return tmp;
            if (!tmp.isMap()) return null;
            curr = tmp.map();
        }
    }

    public boolean isMap(String path) {
        var el = get(path);
        return el != null && el.isMap();
    }
    public boolean isList(String path) {
        var el = get(path);
        return el != null && el.isList();
    }
    public boolean isString(String path) {
        var el = get(path);
        return el != null && el.isString();
    }
    public boolean isNumber(String path) {
        var el = get(path);
        return el != null && el.isNumber();
    }
    public boolean isBoolean(String path) {
        var el = get(path);
        return el != null && el.isBoolean();
    }
    public boolean isNull(String path) {
        var el = get(path);
        return el != null && el.isNull();
    }
    public boolean contains(String path) {
        return get(path) != null;
    }

    public JSONMap map(String path) {
        var el = get(path);
        if (el == null) throw new RuntimeException(String.format("'%s' doesn't exist", path));
        return el.map();
    }
    public JSONMap map(String path, JSONMap defaultVal) {
        var el = get(path);
        if (el == null) return defaultVal;
        if (el.isMap()) return el.map();
        return defaultVal;
    }

    public JSONList list(String path) {
        var el = get(path);
        if (el == null) throw new RuntimeException(String.format("'%s' doesn't exist", path));
        return el.list();
    }
    public JSONList list(String path, JSONList defaultVal) {
        var el = get(path);
        if (el == null) return defaultVal;
        if (el.isList()) return el.list();
        return defaultVal;
    }

    public String string(String path) {
        var el = get(path);
        if (el == null) throw new RuntimeException(String.format("'%s' doesn't exist", path));
        return el.string();
    }
    public String string(String path, String defaultVal) {
        var el = get(path);
        if (el == null) return defaultVal;
        if (el.isString()) return el.string();
        return defaultVal;
    }

    public double number(String path) {
        var el = get(path);
        if (el == null) throw new RuntimeException(String.format("'%s' doesn't exist", path));
        return el.number();
    }
    public double number(String path, double defaultVal) {
        var el = get(path);
        if (el == null) return defaultVal;
        if (el.isNumber()) return el.number();
        return defaultVal;
    }

    public boolean bool(String path) {
        var el = get(path);
        if (el == null) throw new RuntimeException(String.format("'%s' doesn't exist", path));
        return el.bool();
    }
    public boolean bool(String path, boolean defaultVal) {
        var el = get(path);
        if (el == null) return defaultVal;
        if (el.isBoolean()) return el.bool();
        return defaultVal;
    }

    public JSONMap setNull(String key) { elements.put(key, JSONElement.NULL); return this; }
    public JSONMap set(String key, String val) { elements.put(key, JSONElement.of(val)); return this; }
    public JSONMap set(String key, double val) { elements.put(key, JSONElement.of(val)); return this; }
    public JSONMap set(String key, boolean val) { elements.put(key, JSONElement.of(val)); return this; }
    public JSONMap set(String key, Map<String, JSONElement> val) { elements.put(key, JSONElement.of(val)); return this; }
    public JSONMap set(String key, Collection<JSONElement> val) { elements.put(key, JSONElement.of(val)); return this; }

    @Override public int size() { return elements.size(); }
    @Override public boolean isEmpty() { return elements.isEmpty(); }
    @Override public boolean containsKey(Object key) { return elements.containsKey(key); }
    @Override public boolean containsValue(Object value) { return elements.containsValue(value); }
    @Override public JSONElement get(Object key) { return elements.get(key); }
    @Override public JSONElement put(String key, JSONElement value) { return elements.put(key, value); }
    @Override public JSONElement remove(Object key) { return elements.remove(key); }
    @Override public void putAll(Map<? extends String, ? extends JSONElement> m) { elements.putAll(m); }

    @Override public void clear() { elements.clear(); }

    @Override public Set<String> keySet() { return elements.keySet(); }
    @Override public Collection<JSONElement> values() { return elements.values(); }
    @Override public Set<Entry<String, JSONElement>> entrySet() { return elements.entrySet(); }

    public JSONMap() { }
    public JSONMap(Map<String, JSONElement> els) {
        this.elements = new HashMap<>(els);
    }
}
