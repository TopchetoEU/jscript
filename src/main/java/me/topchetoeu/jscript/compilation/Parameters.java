package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.patterns.Pattern;
import me.topchetoeu.jscript.compilation.values.operations.AssignNode;

public final class Parameters {
    public final int length;
    public final List<Pattern> params;
    public final Pattern rest;

    public Parameters(List<Pattern> params, Pattern rest) {
        var len = params.size();

        for (var i = params.size() - 1; i >= 0; i--) {
            if (!(params.get(i) instanceof AssignNode)) break;
            len--;
        }

        this.params = params;
        this.length = len;
        this.rest = rest;
    }
    public Parameters(List<Pattern> params) {
        this(params, null);
    }

    public static ParseRes<Parameters> parseParameters(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        var openParen = Parsing.parseOperator(src, i + n, "(");
        if (!openParen.isSuccess()) return openParen.chainError(src.loc(i + n), "Expected a parameter list");
        n += openParen.n;

        var params = new ArrayList<Pattern>();

        var closeParen = Parsing.parseOperator(src, i + n, ")");
        n += closeParen.n;

        if (!closeParen.isSuccess()) {
            while (true) {
                n += Parsing.skipEmpty(src, i + n);

                if (src.is(i + n, "...")) {
                    n += 3;

                    var rest = Pattern.parse(src, i + n, true);
                    if (!rest.isSuccess()) return ParseRes.error(src.loc(i + n), "Expected a rest parameter");
                    n += rest.n;
                    n += Parsing.skipEmpty(src, i + n);

                    if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n),  "Expected an end of parameters list after rest parameter");
                    n++;

                    return ParseRes.res(new Parameters(params, rest.result), n);
                }

                var param = Pattern.parse(src, i + n, true);
                if (!param.isSuccess()) return ParseRes.error(src.loc(i + n), "Expected a parameter or a closing brace");
                n += param.n;
                n += Parsing.skipEmpty(src, i + n);

                params.add(param.result);

                if (src.is(i + n, ",")) {
                    n++;
                    n += Parsing.skipEmpty(src, i + n);
                }

                if (src.is(i + n, ")")) {
                    n++;
                    break;
                }
            }
        }

        return ParseRes.res(new Parameters(params), n);
    }
}
