package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.parsing.Location;

public abstract class AssignableStatement extends Statement {
    public abstract Statement toAssign(Statement val, Operation operation);

    protected AssignableStatement(Location loc) {
        super(loc);
    }
    // private static final Map<String, Operation> operations = Map.ofEntries(
    //     Map.entry("*=", Operation.MULTIPLY),
    //     Map.entry("/=", Operation.DIVIDE),
    //     Map.entry("%=", Operation.MODULO),
    //     Map.entry("-=", Operation.SUBTRACT),
    //     Map.entry("+=", Operation.ADD),
    //     Map.entry(">>=", Operation.SHIFT_RIGHT),
    //     Map.entry("<<=", Operation.SHIFT_LEFT),
    //     Map.entry(">>>=", Operation.USHIFT_RIGHT),
    //     Map.entry("&=", Operation.AND),
    //     Map.entry("^=", Operation.XOR),
    //     Map.entry("|=", Operation.OR)
    // );
    // private static final List<String> operatorsByLength = operations.keySet().stream().sorted().collect(Collectors.toList());


    // public static ParseRes<? extends Statement> parse(Source src, int i, Statement prev, int precedence) {
    //     if (precedence > 2) return ParseRes.failed();

    //     var n = Parsing.skipEmpty(src, i);

    //     for (var op : operatorsByLength) {
    //         if (!src.is(i + n, op)) continue;
    //         n += op.length() + 1;

    //         if (!(prev instanceof AssignableStatement)) {
    //             return ParseRes.error(src.loc(i + n), "Invalid expression on left hand side of assign operator");
    //         }

    //         var res = Parsing.parseValue(src, i + n, 2);
    //         if (!res.isSuccess()) return res.chainError(src.loc(i + n), String.format("Expected a value after the '%s=' operator", op));
    //         n += res.n;

    //         return ParseRes.res(((AssignableStatement)prev).toAssign(res.result, operations.get(op)), n);
    //     }

    //     if (!src.is(i + n, "=")) return ParseRes.failed();
    //     n++;

    //     if (!(prev instanceof AssignableStatement)) {
    //         return ParseRes.error(src.loc(i + n), "Invalid expression on left hand side of assign operator");
    //     }

    //     var res = Parsing.parseValue(src, i + n, 2);
    //     if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected a value after the '=' operator");
    //     n += res.n;

    //     return ParseRes.res(((AssignableStatement)prev).toAssign(res.result, null), n);
    // }
}
