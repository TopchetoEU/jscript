package me.topchetoeu.jscript.lib;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeGetter;

public class MapLib {
    private LinkedHashMap<Object, Object> map = new LinkedHashMap<>();

    @Native("@@Symbol.typeName") public final String name = "Map";
    @Native("@@Symbol.iterator") public ObjectValue iterator(Context ctx) {
        return this.entries(ctx);
    }

    @Native public void clear() {
        map.clear();
    }
    @Native public boolean delete(Object key) {
        if (map.containsKey(key)) {
            map.remove(key);
            return true;
        }
        return false;
    }

    @Native public ObjectValue entries(Context ctx) {
        var res = map.entrySet().stream().map(v -> {
            return new ArrayValue(ctx, v.getKey(), v.getValue());
        }).collect(Collectors.toList());
        return Values.fromJavaIterator(ctx, res.iterator());
    }
    @Native public ObjectValue keys(Context ctx) {
        var res = new ArrayList<>(map.keySet());
        return Values.fromJavaIterator(ctx, res.iterator());
    }
    @Native public ObjectValue values(Context ctx) {
        var res = new ArrayList<>(map.values());
        return Values.fromJavaIterator(ctx, res.iterator());
    }

    @Native public Object get(Object key) {
        return map.get(key);
    }
    @Native public MapLib set(Object key, Object val) {
        map.put(key, val);
        return this;
    }
    @Native public boolean has(Object key) {
        return map.containsKey(key);
    }

    @NativeGetter public int size() {
        return map.size();
    }

    @NativeGetter public void forEach(Context ctx, FunctionValue func, Object thisArg) {
        var keys = new ArrayList<>(map.keySet());

        for (var el : keys) func.call(ctx, thisArg, el, map.get(el), this);
    }

    @Native public MapLib(Context ctx, Object iterable) {
        for (var el : Values.toJavaIterable(ctx, iterable)) {
            try {
                set(Values.getMember(ctx, el, 0), Values.getMember(ctx, el, 1));
            }
            catch (IllegalArgumentException e) { }
        }
    }
}
