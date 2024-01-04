package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.exceptions.SyntaxException;
import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.Expose;
import me.topchetoeu.jscript.interop.ExposeTarget;
import me.topchetoeu.jscript.interop.WrapperName;
import me.topchetoeu.jscript.json.JSON;

@WrapperName("JSON")
public class JSONLib {
    @Expose(target = ExposeTarget.STATIC)
    public static Object __parse(Arguments args) {
        try {
            return JSON.toJs(JSON.parse(null, args.getString(0)));
        }
        catch (SyntaxException e) { throw EngineException.ofSyntax(e.msg); }
    }
    @Expose(target = ExposeTarget.STATIC)
    public static String __stringify(Arguments args) {
        return JSON.stringify(JSON.fromJs(args.ctx, args.get(0)));
    }
}
