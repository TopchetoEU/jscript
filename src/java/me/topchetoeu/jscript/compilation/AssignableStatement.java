package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.ParseRes;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Source;

public abstract class AssignableStatement extends Statement {
    public abstract Statement toAssign(Statement val, Operation operation);

    protected AssignableStatement(Location loc) {
        super(loc);
    }

    public static ParseRes<? extends Statement> parse(Source src, int i, Statement prev, int precedence) {
        if (precedence > 2) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);

        for (var op : Operator.opsByLength) {
            if (!op.assignable || !src.is(i + n, op.readable + "=")) continue;
            n += op.readable.length() + 1;

            if (!(prev instanceof AssignableStatement)) {
                return ParseRes.error(src.loc(i + n), "Invalid expression on left hand side of assign operator");
            }

            var res = Parsing.parseValue(src, i + n, 2);
            if (!res.isSuccess()) return res.chainError(src.loc(i + n), String.format("Expected a value after the '%s=' operator", op.readable));
            n += res.n;

            return ParseRes.res(((AssignableStatement)prev).toAssign(res.result, op.operation), n);
        }

        if (!src.is(i + n, "=")) return ParseRes.failed();
        n++;

        if (!(prev instanceof AssignableStatement)) {
            return ParseRes.error(src.loc(i + n), "Invalid expression on left hand side of assign operator");
        }

        var res = Parsing.parseValue(src, i + n, 2);
        if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected a value after the '=' operator");
        n += res.n;

        return ParseRes.res(((AssignableStatement)prev).toAssign(res.result, null), n);
    }
}
