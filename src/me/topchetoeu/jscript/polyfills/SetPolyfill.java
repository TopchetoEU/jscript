package me.topchetoeu.jscript.polyfills;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.interop.NativeGetter;

public class SetPolyfill {
    private LinkedHashSet<Object> set = new LinkedHashSet<>();

    @Native("@@Symbol.iterator") public ObjectValue iterator(Context ctx) throws InterruptedException {
        return this.values(ctx);
    }

    @Native public ObjectValue entries(Context ctx) throws InterruptedException {
        var res = set.stream().map(v -> new ArrayValue(ctx, v, v)).collect(Collectors.toList());
        return Values.fromJavaIterator(ctx, res.iterator());
    }
    @Native public ObjectValue keys(Context ctx) throws InterruptedException {
        var res = new ArrayList<>(set);
        return Values.fromJavaIterator(ctx, res.iterator());
    }
    @Native public ObjectValue values(Context ctx) throws InterruptedException {
        var res = new ArrayList<>(set);
        return Values.fromJavaIterator(ctx, res.iterator());
    }

    @Native public Object add(Object key) {
        return set.add(key);
    }
    @Native public boolean delete(Object key) {
        return set.remove(key);
    }
    @Native public boolean has(Object key) {
        return set.contains(key);
    }

    @Native public void clear() {
        set.clear();
    }

    @NativeGetter public int size() {
        return set.size();
    }

    @NativeGetter public void forEach(Context ctx, FunctionValue func, Object thisArg) throws InterruptedException {
        var keys = new ArrayList<>(set);

        for (var el : keys) func.call(ctx, thisArg, el, el, this);
    }

    @Native public SetPolyfill(Context ctx, Object iterable) throws InterruptedException {
        for (var el : Values.toJavaIterable(ctx, iterable)) add(el);
    }
}
