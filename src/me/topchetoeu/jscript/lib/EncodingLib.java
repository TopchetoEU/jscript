package me.topchetoeu.jscript.lib;

import me.topchetoeu.jscript.engine.Context;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.interop.Native;

@Native("Encoding")
public class EncodingLib {
    @Native public static ArrayValue encode(String value) {
        var res = new ArrayValue();
        for (var el : value.getBytes()) res.set(null, res.size(), (int)el);
        return res;
    }
    @Native public static String decode(Context ctx, ArrayValue raw) {
        var res = new byte[raw.size()];
        for (var i = 0; i < raw.size(); i++) res[i] = (byte)Values.toNumber(ctx, raw.get(i));
        return new String(res);
    }
}
