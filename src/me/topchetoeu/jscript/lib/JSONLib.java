package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.exceptions.SyntaxException;
import me.topchetoeu.jscript.interop.Native;
import me.topchetoeu.jscript.json.JSON;

@Native("JSON") public class JSONLib {
    @Native
    public static Object parse(Context ctx, String val) {
        try {
            return JSON.toJs(JSON.parse(null, val));
        }
        catch (SyntaxException e) { throw EngineException.ofSyntax(e.msg); }
    }
    @Native
    public static String stringify(Context ctx, Object val) {
        return me.topchetoeu.jscript.json.JSON.stringify(JSON.fromJs(ctx, val));
    }
}
