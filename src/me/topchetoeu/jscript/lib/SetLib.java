package me.topchetoeu.jscript.lib;

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

public class SetLib {
    private LinkedHashSet<Object> set = new LinkedHashSet<>();

    @Native("@@Symbol.typeName") public final String name = "Set";
    @Native("@@Symbol.iterator") public ObjectValue iterator(Context ctx) {
        return this.values(ctx);
    }

    @Native public ObjectValue entries(Context ctx) {
        var res = set.stream().map(v -> new ArrayValue(ctx, v, v)).collect(Collectors.toList());
        return Values.fromJavaIterator(ctx, res.iterator());
    }
    @Native public ObjectValue keys(Context ctx) {
        var res = new ArrayList<>(set);
        return Values.fromJavaIterator(ctx, res.iterator());
    }
    @Native public ObjectValue values(Context ctx) {
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

    @Native public void forEach(Context ctx, FunctionValue func, Object thisArg) {
        var keys = new ArrayList<>(set);

        for (var el : keys) func.call(ctx, thisArg, el, el, this);
    }

    @Native public SetLib(Context ctx, Object iterable) {
        for (var el : Values.toJavaIterable(ctx, iterable)) add(el);
    }
}
