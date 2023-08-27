package me.topchetoeu.jscript.polyfills;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeGetter;

public class Map {
    private LinkedHashMap<Object, Object> objs = new LinkedHashMap<>();

    @Native
    public Object get(Object key) {
        return objs.get(key);
    }
    @Native
    public Map set(Object key, Object val) {
        objs.put(key, val);
        return this;
    }
    @Native
    public boolean delete(Object key) {
        if (objs.containsKey(key)) {
            objs.remove(key);
            return true;
        }
        else return false;
    }
    @Native
    public boolean has(Object key) {
        return objs.containsKey(key);
    }

    @Native
    public void clear() {
        objs.clear();
    }

    @Native
    public void forEach(CallContext ctx, FunctionValue func, Object thisArg) throws InterruptedException {

        for (var el : objs.entrySet().stream().collect(Collectors.toList())) {
            func.call(ctx, thisArg, el.getValue(), el.getKey(), this);
        }
    }

    @Native
    public Object entries(CallContext ctx) throws InterruptedException {
        return Values.fromJavaIterable(ctx, objs
            .entrySet()
            .stream()
            .map(v -> new ArrayValue(ctx, v.getKey(), v.getValue()))
            .collect(Collectors.toList())
        );
    }
    @Native
    public Object keys(CallContext ctx) throws InterruptedException {
        return Values.fromJavaIterable(ctx, objs.keySet().stream().collect(Collectors.toList()));
    }
    @Native
    public Object values(CallContext ctx) throws InterruptedException {
        return Values.fromJavaIterable(ctx, objs.values().stream().collect(Collectors.toList()));
    }

    @NativeGetter("size")
    public int size() {
        return objs.size();
    }

    @Native
    public Map(CallContext ctx, Object iterable) throws InterruptedException {
        if (Values.isPrimitive(iterable)) return;

        for (var val : Values.toJavaIterable(ctx, iterable)) {
            var first = Values.getMember(ctx, val, 0);
            var second = Values.getMember(ctx, val, 1);

            set(first, second);
        }
    }
    public Map() { }
}
