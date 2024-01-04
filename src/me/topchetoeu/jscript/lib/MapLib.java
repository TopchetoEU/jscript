package me.topchetoeu.jscript.lib;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.FunctionValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.Expose;
import me.topchetoeu.jscript.interop.ExposeConstructor;
import me.topchetoeu.jscript.interop.ExposeType;
import me.topchetoeu.jscript.interop.WrapperName;

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
        return ArrayValue.of(args.ctx, map
            .entrySet()
            .stream()
            .map(v -> new ArrayValue(args.ctx, v.getKey(), v.getValue()))
            .collect(Collectors.toList())
        );
    }
    @Expose public ObjectValue __keys(Arguments args) {
        return ArrayValue.of(args.ctx, map.keySet());
    }
    @Expose public ObjectValue __values(Arguments args) {
        return ArrayValue.of(args.ctx, map.values());
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
        var func = args.convert(0, FunctionValue.class);
        var thisArg = args.get(1);

        var keys = new ArrayList<>(map.keySet());

        for (var el : keys) func.call(args.ctx, thisArg, map.get(el), el,this);
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
