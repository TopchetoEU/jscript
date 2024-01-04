package me.topchetoeu.jscript.lib;

import java.util.ArrayList;

import me.topchetoeu.jscript.Buffer;
import me.topchetoeu.jscript.engine.values.ArrayValue;
import me.topchetoeu.jscript.engine.values.Values;
import me.topchetoeu.jscript.exceptions.EngineException;
import me.topchetoeu.jscript.interop.Arguments;
import me.topchetoeu.jscript.interop.Expose;
import me.topchetoeu.jscript.interop.ExposeTarget;
import me.topchetoeu.jscript.interop.WrapperName;
import me.topchetoeu.jscript.parsing.Parsing;

@WrapperName("Encoding")
public class EncodingLib {
    private static final String HEX = "0123456789ABCDEF";

    public static String encodeUriAny(String str, String keepAlphabet) {
        if (str == null) str = "undefined";

        var bytes = str.getBytes();
        var sb = new StringBuilder(bytes.length);

        for (byte c : bytes) {
            if (Parsing.isAlphanumeric((char)c) || Parsing.isAny((char)c, keepAlphabet)) sb.append((char)c);
            else {
                sb.append('%');
                sb.append(HEX.charAt(c / 16));
                sb.append(HEX.charAt(c % 16));
            }
        }

        return sb.toString();
    }
    public static String decodeUriAny(String str, String keepAlphabet) {
        if (str == null) str = "undefined";

        var res = new Buffer();
        var bytes = str.getBytes();

        for (var i = 0; i < bytes.length; i++) {
            var c = bytes[i];
            if (c == '%') {
                if (i >= bytes.length - 2) throw EngineException.ofError("URIError", "URI malformed.");
                var b = Parsing.fromHex((char)bytes[i + 1]) * 16 | Parsing.fromHex((char)bytes[i + 2]);
                if (!Parsing.isAny((char)b, keepAlphabet)) {
                    i += 2;
                    res.append((byte)b);
                    continue;
                }
            }
            res.append(c);
        }

        return new String(res.data());
    }

    @Expose(target = ExposeTarget.STATIC)
    public static ArrayValue __encode(Arguments args) {
        var res = new ArrayValue();
        for (var el : args.getString(0).getBytes()) res.set(null, res.size(), (int)el);
        return res;
    }
    @Expose(target = ExposeTarget.STATIC)
    public static String __decode(Arguments args) {
        var raw = args.convert(0, ArrayList.class);
        var res = new byte[raw.size()];
        for (var i = 0; i < raw.size(); i++) res[i] = (byte)Values.toNumber(args.ctx, raw.get(i));
        return new String(res);
    }

    @Expose(target = ExposeTarget.STATIC)
    public static String __encodeURIComponent(Arguments args) {
        return EncodingLib.encodeUriAny(args.getString(0), ".-_!~*'()");
    }
    @Expose(target = ExposeTarget.STATIC)
    public static String __decodeURIComponent(Arguments args) {
        return decodeUriAny(args.getString(0), "");
    }
    @Expose(target = ExposeTarget.STATIC)
    public static String __encodeURI(Arguments args) {
        return encodeUriAny(args.getString(0), ";,/?:@&=+$#.-_!~*'()");
    }
    @Expose(target = ExposeTarget.STATIC)
    public static String __decodeURI(Arguments args) {
        return decodeUriAny(args.getString(0), ",/?:@&=+$#.");
    }
}
