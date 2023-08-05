package me.topchetoeu.jscript.polyfills;

import me.topchetoeu.jscript.engine.CallContext;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.ObjectValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.exceptions.SyntaxException;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.json.JSONElement;

public class JSON {
    private static Object convert(JSONElement val) {
        if (val.isBoolean()) return val.bool();
        if (val.isString()) return val.string();
        if (val.isNumber()) return val.number();
        if (val.isList()) return ArrayValue.of(val.list().stream().map(JSON::convert).toList());
        if (val.isMap()) {
            var res = new ObjectValue();
            for (var el : val.map().entrySet()) {
                res.defineProperty(el.getKey(), convert(el.getValue()));
            }
            return res;
        }
        if (val.isNull()) return Values.NULL;
        return null;
    }

    @Native
    public static Object parse(CallContext ctx, String val) throws InterruptedException {
        try {
            return convert(me.topchetoeu.jscript.json.JSON.parse("<value>", val));
        }
        catch (SyntaxException e) {
            throw EngineException.ofSyntax(e.msg);
        }
    }
}
