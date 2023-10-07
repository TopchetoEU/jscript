package me.topchetoeu.jscript.lib;

import java.util.HashSet;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.exceptions.SyntaxException;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.json.JSONElement;
import me.topchetoeu.jscript.json.JSONList;
import me.topchetoeu.jscript.json.JSONMap;

public class JSONLib {
    private static Object toJS(JSONElement val) {
        if (val.isBoolean()) return val.bool();
        if (val.isString()) return val.string();
        if (val.isNumber()) return val.number();
        if (val.isList()) return ArrayValue.of(null, val.list().stream().map(JSONLib::toJS).collect(Collectors.toList()));
        if (val.isMap()) {
            var res = new ObjectValue();
            for (var el : val.map().entrySet()) {
                res.defineProperty(null, el.getKey(), toJS(el.getValue()));
            }
            return res;
        }
        if (val.isNull()) return Values.NULL;
        return null;
    }
    private static JSONElement toJSON(Context ctx, Object val, HashSet<Object> prev) {
        if (val instanceof Boolean) return JSONElement.bool((boolean)val);
        if (val instanceof Number) return JSONElement.number(((Number)val).doubleValue());
        if (val instanceof String) return JSONElement.string((String)val);
        if (val == Values.NULL) return JSONElement.NULL;
        if (val instanceof ObjectValue) {
            if (prev.contains(val)) throw new EngineException("Circular dependency in JSON.");
            prev.add(val);

            var res = new JSONMap();

            for (var el : ((ObjectValue)val).keys(false)) {
                var jsonEl = toJSON(ctx, ((ObjectValue)val).getMember(ctx, el), prev);
                if (jsonEl == null) continue;
                if (el instanceof String || el instanceof Number) res.put(el.toString(), jsonEl);
            }

            prev.remove(val);
            return JSONElement.of(res);
        }
        if (val instanceof ArrayValue) {
            if (prev.contains(val)) throw new EngineException("Circular dependency in JSON.");
            prev.add(val);

            var res = new JSONList();

            for (var el : ((ArrayValue)val).toArray()) {
                var jsonEl = toJSON(ctx, el, prev);
                if (jsonEl == null) jsonEl = JSONElement.NULL;
                res.add(jsonEl);
            }

            prev.remove(val);
            return JSONElement.of(res);
        }
        if (val == null) return null;
        return null;
    }

    @Native
    public static Object parse(Context ctx, String val) {
        try {
            return toJS(me.topchetoeu.jscript.json.JSON.parse("<value>", val));
        }
        catch (SyntaxException e) { throw EngineException.ofSyntax(e.msg); }
    }
    @Native
    public static String stringify(Context ctx, Object val) {
        return me.topchetoeu.jscript.json.JSON.stringify(toJSON(ctx, val, new HashSet<>()));
    }
}
