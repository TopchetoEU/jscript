package me.topchetoeu.jscript.lib;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.Expose;
import me.topchetoeu.jscript.interop.ExposeConstructor;
import me.topchetoeu.jscript.interop.ExposeType;
import me.topchetoeu.jscript.interop.WrapperName;

@WrapperName("Set")
public class SetLib {
    private LinkedHashSet<Object> set = new LinkedHashSet<>();

    @Expose("@@Symbol.iterator")
    public ObjectValue __iterator(Arguments args) {
        return this.__values(args);
    }

    @Expose public ObjectValue __entries(Arguments args) {
        return ArrayValue.of(args.ctx, set.stream().map(v -> new ArrayValue(args.ctx, v, v)).collect(Collectors.toList()));
    }
    @Expose public ObjectValue __keys(Arguments args) {
        return ArrayValue.of(args.ctx, set);
    }
    @Expose public ObjectValue __values(Arguments args) {
        return ArrayValue.of(args.ctx, set);
    }

    @Expose public Object __add(Arguments args) {
        return set.add(args.get(0));
    }
    @Expose public boolean __delete(Arguments args) {
        return set.remove(args.get(0));
    }
    @Expose public boolean __has(Arguments args) {
        return set.contains(args.get(0));
    }

    @Expose public void __clear() {
        set.clear();
    }

    @Expose(type = ExposeType.GETTER)
    public int __size() {
        return set.size();
    }

    @Expose public void __forEach(Arguments args) {
        var keys = new ArrayList<>(set);

        for (var el : keys) Values.call(args.ctx, args.get(0), args.get(1), el, el, this);
    }

    public SetLib(Context ctx, Object iterable) {
        for (var el : Values.fromJSIterator(ctx, iterable)) set.add(el);
    }

    @ExposeConstructor
    public static SetLib __constructor(Arguments args) {
        return new SetLib(args.ctx, args.get(0));
    }
}
