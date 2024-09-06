package me.topchetoeu.jscript.runtime;

import java.util.HashSet;
import java.util.stream.Collectors;

import me.topchetoeu.jscript.common.environment.Environment;
import me.topchetoeu.jscript.common.json.JSONElement;
import me.topchetoeu.jscript.common.json.JSONList;
import me.topchetoeu.jscript.common.json.JSONMap;
import me.topchetoeu.jscript.runtime.exceptions.EngineException;
import me.topchetoeu.jscript.runtime.values.Member.FieldMember;
import me.topchetoeu.jscript.runtime.values.Value;
import me.topchetoeu.jscript.runtime.values.objects.ArrayValue;
import me.topchetoeu.jscript.runtime.values.objects.ObjectValue;
import me.topchetoeu.jscript.runtime.values.primitives.BoolValue;
import me.topchetoeu.jscript.runtime.values.primitives.NumberValue;
import me.topchetoeu.jscript.runtime.values.primitives.StringValue;
import me.topchetoeu.jscript.runtime.values.primitives.VoidValue;

public class JSONConverter {
    public static Value toJs(JSONElement val) {
        if (val.isBoolean()) return BoolValue.of(val.bool());
        if (val.isString()) return new StringValue(val.string());
        if (val.isNumber()) return new NumberValue(val.number());
        if (val.isList()) return ArrayValue.of(val.list().stream().map(JSONConverter::toJs).collect(Collectors.toList()));
        if (val.isMap()) {
            var res = new ObjectValue();
    
            for (var el : val.map().entrySet()) {
                res.defineOwnMember(null, el.getKey(), FieldMember.of(toJs(el.getValue())));
            }
    
            return res;
        }
        if (val.isNull()) return Value.NULL;
        return Value.UNDEFINED;
    }

    public static JSONElement fromJs(Environment ext, Value val) {
        var res = JSONConverter.fromJs(ext, val, new HashSet<>());
        if (res == null) return JSONElement.NULL;
        else return res;
    }

    public static JSONElement fromJs(Environment env, Value val, HashSet<Object> prev) {
        if (val instanceof BoolValue) return JSONElement.bool(((BoolValue)val).value);
        if (val instanceof NumberValue) return JSONElement.number(((NumberValue)val).value);
        if (val instanceof StringValue) return JSONElement.string(((StringValue)val).value);
        if (val == Value.NULL) return JSONElement.NULL;
        if (val instanceof VoidValue) return null;
    
        if (val instanceof ArrayValue) {
            if (prev.contains(val)) throw EngineException.ofError("Circular dependency in JSON.");
            prev.add(val);
    
            var res = new JSONList();
    
            for (var el : ((ArrayValue)val).toArray()) {
                var jsonEl = fromJs(env, el, prev);
                if (jsonEl == null) continue;
                res.add(jsonEl);
            }
    
            prev.remove(val);
            return JSONElement.of(res);
        }
        if (val instanceof ObjectValue) {
            if (prev.contains(val)) throw EngineException.ofError("Circular dependency in JSON.");
            prev.add(val);
    
            var res = new JSONMap();
    
            for (var key : val.getOwnMembers(env, true)) {
                var el = fromJs(env, val.getMember(env, key), prev);
                if (el == null) continue;

                res.put(key, el);
            }
    
            prev.remove(val);
            return JSONElement.of(res);
        }
        if (val == null) return null;
        return null;
    }
    
}
