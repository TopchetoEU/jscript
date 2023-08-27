package me.topchetoeu.jscript.polyfills;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeGetter;

public class Set {
    private LinkedHashSet<Object> objs = new LinkedHashSet<>();

    @Native
    public Set add(Object key) {
        objs.add(key);
        return this;
    }
    @Native
    public boolean delete(Object key) {
        return objs.remove(key);
    }
    @Native
    public boolean has(Object key) {
        return objs.contains(key);
    }
    @Native
    public void clear() {
        objs.clear();
    }

    @Native
    public void forEach(CallContext ctx, Object func, Object thisArg) throws InterruptedException {
        if (!(func instanceof FunctionValue)) throw EngineException.ofType("func must be a function.");

        for (var el : objs.stream().collect(Collectors.toList())) {
            ((FunctionValue)func).call(ctx, thisArg, el, el, this);
        }
    }

    @Native
    public ObjectValue entries(CallContext ctx) throws InterruptedException {
        return Values.fromJavaIterable(ctx, objs
            .stream()
            .map(v -> new ArrayValue(ctx, v, v))
            .collect(Collectors.toList())
        );
    }
    @Native
    public ObjectValue keys(CallContext ctx) throws InterruptedException {
        return Values.fromJavaIterable(ctx, objs);
    }
    @Native
    public ObjectValue values(CallContext ctx) throws InterruptedException {
        return Values.fromJavaIterable(ctx, objs);
    }

    @NativeGetter("size")
    public int size() {
        return objs.size();
    }

    @Native
    public Set(CallContext ctx, Object iterable) throws InterruptedException {
        if (Values.isPrimitive(iterable)) return;

        for (var val : Values.toJavaIterable(ctx, iterable)) {
            add(val);
        }
    }
    public Set() { }
}
