package me.topchetoeu.jscript.lib;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.core.Context;
import me.topchetoeu.jscript.core.values.ArrayValue;
import me.topchetoeu.jscript.core.values.ObjectValue;
import me.topchetoeu.jscript.core.values.Values;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeConstructor;
import me.topchetoeu.jscript.utils.interop.ExposeType;
import me.topchetoeu.jscript.utils.interop.WrapperName;

@WrapperName("Map")
public class MapLib {
    private LinkedHashMap<Object, Object> map = new LinkedHashMap<>();

    @Expose("@@Symbol.iterator")
    public ObjectValue __iterator(Arguments args) {
        return this.__entries(args);
    }

    @Expose public void __clear() {
        map.clear();
    }
    @Expose public boolean __delete(Arguments args) {
        var key = args.get(0);
        if (map.containsKey(key)) {
            map.remove(key);
            return true;
        }
        return false;
    }

    @Expose public ObjectValue __entries(Arguments args) {
        return Values.toJSIterator(args.ctx, map
            .entrySet()
            .stream()
            .map(v -> new ArrayValue(args.ctx, v.getKey(), v.getValue()))
            .collect(Collectors.toList())
        );
    }
    @Expose public ObjectValue __keys(Arguments args) {
        return Values.toJSIterator(args.ctx, map.keySet());
    }
    @Expose public ObjectValue __values(Arguments args) {
        return Values.toJSIterator(args.ctx, map.values());
    }

    @Expose public Object __get(Arguments args) {
        return map.get(args.get(0));
    }
    @Expose public MapLib __set(Arguments args) {
        map.put(args.get(0), args.get(1));
        return this;
    }
    @Expose public boolean __has(Arguments args) {
        return map.containsKey(args.get(0));
    }

    @Expose(type = ExposeType.GETTER)
    public int __size() {
        return map.size();
    }

    @Expose public void __forEach(Arguments args) {
        var keys = new ArrayList<>(map.keySet());

        for (var el : keys) Values.call(args.ctx, args.get(0), args.get(1), map.get(el), el, args.self);
    }

    public MapLib(Context ctx, Object iterable) {
        for (var el : Values.fromJSIterator(ctx, iterable)) {
            try {
                map.put(Values.getMember(ctx, el, 0), Values.getMember(ctx, el, 1));
            }
            catch (IllegalArgumentException e) { }
        }
    }

    @ExposeConstructor public static MapLib __constructor(Arguments args) {
        return new MapLib(args.ctx, args.get(0));
    }
}
