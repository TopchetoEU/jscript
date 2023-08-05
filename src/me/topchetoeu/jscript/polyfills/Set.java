package me.topchetoeu.jscript.polyfills;

import java.util.LinkedHashSet;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.NativeFunction;
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

        for (var el : objs.stream().toList()) {
            ((FunctionValue)func).call(ctx, thisArg, el, el, this);
        }
    }

    @Native
    public ObjectValue entries() {
        var it = objs.stream().toList().iterator();

        var next = new NativeFunction("next", (ctx, thisArg, args) -> {
            if (it.hasNext()) {
                var val = it.next();
                return new ObjectValue(java.util.Map.of(
                    "value", new ArrayValue(val, val),
                    "done", false
                ));
            }
            else return new ObjectValue(java.util.Map.of("done", true));
        });

        return new ObjectValue(java.util.Map.of("next", next));
    }
    @Native
    public ObjectValue keys() {
        var it = objs.stream().toList().iterator();

        var next = new NativeFunction("next", (ctx, thisArg, args) -> {
            if (it.hasNext()) {
                var val = it.next();
                return new ObjectValue(java.util.Map.of(
                    "value", val,
                    "done", false
                ));
            }
            else return new ObjectValue(java.util.Map.of("done", true));
        });

        return new ObjectValue(java.util.Map.of("next", next));
    }
    @Native
    public ObjectValue values() {
        var it = objs.stream().toList().iterator();

        var next = new NativeFunction("next", (ctx, thisArg, args) -> {
            if (it.hasNext()) {
                var val = it.next();
                return new ObjectValue(java.util.Map.of(
                    "value", val,
                    "done", false
                ));
            }
            else return new ObjectValue(java.util.Map.of("done", true));
        });

        return new ObjectValue(java.util.Map.of("next", next));
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
