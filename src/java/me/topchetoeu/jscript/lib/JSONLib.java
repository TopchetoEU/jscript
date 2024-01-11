package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.common.json.JSON;
import me.topchetoeu.jscript.core.exceptions.EngineException;
import me.topchetoeu.jscript.core.exceptions.SyntaxException;
import me.topchetoeu.jscript.utils.interop.Arguments;
import me.topchetoeu.jscript.utils.interop.Expose;
import me.topchetoeu.jscript.utils.interop.ExposeTarget;
import me.topchetoeu.jscript.utils.interop.WrapperName;

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
