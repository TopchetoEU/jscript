package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.ParseRes;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Source;

public class OperationStatement extends Statement {
    public final Statement[] args;
    public final Operation operation;

    @Override public boolean pure() {
        for (var el : args) {
            if (!el.pure()) return false;
        }

        return true;
    }

    @Override public void compile(CompileResult target, boolean pollute) {
        for (var arg : args) {
            arg.compile(target, true);
        }

        if (pollute) target.add(Instruction.operation(operation));
        else target.add(Instruction.discard());
    }

    public OperationStatement(Location loc, Operation operation, Statement ...args) {
        super(loc);
        this.operation = operation;
        this.args = args;
    }

    public static ParseRes<OperationStatement> parsePrefix(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        Operation operation = null;
        String op;

        if (src.is(i + n, op = "+")) operation = Operation.POS;
        else if (src.is(i + n, op = "-")) operation = Operation.NEG;
        else if (src.is(i + n, op = "~")) operation = Operation.INVERSE;
        else if (src.is(i + n, op = "!")) operation = Operation.NOT;
        else return ParseRes.failed();

        n++;

        var res = Parsing.parseValue(src, i + n, 14);

        if (res.isSuccess()) return ParseRes.res(new OperationStatement(loc, operation, res.result), n + res.n);
        else return res.chainError(src.loc(i + n), String.format("Expected a value after the unary operator '%s'.", op));
    }
    public static ParseRes<OperationStatement> parseInstanceof(Source src, int i, Statement prev, int precedence) {
        if (precedence > 9) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var kw = Parsing.parseIdentifier(src, i + n, "instanceof");
        if (!kw.isSuccess()) return kw.chainError();
        n += kw.n;

        var valRes = Parsing.parseValue(src, i + n, 10);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'instanceof'.");
        n += valRes.n;

        return ParseRes.res(new OperationStatement(loc, Operation.INSTANCEOF, prev, valRes.result), n);
    }
    public static ParseRes<OperationStatement> parseIn(Source src, int i, Statement prev, int precedence) {
        if (precedence > 9) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var kw = Parsing.parseIdentifier(src, i + n, "in");
        if (!kw.isSuccess()) return kw.chainError();
        n += kw.n;

        var valRes = Parsing.parseValue(src, i + n, 10);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'in'.");
        n += valRes.n;

        return ParseRes.res(new OperationStatement(loc, Operation.IN, valRes.result, prev), n);
    }
    public static ParseRes<? extends Statement> parseOperator(Source src, int i, Statement prev, int precedence) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        for (var op : Operator.opsByLength) {
            if (!src.is(i + n, op.readable)) continue;
            if (op.precedence < precedence) return ParseRes.failed();
            n += op.readable.length();

            var res = Parsing.parseValue(src, i + n, op.precedence + 1);
            if (!res.isSuccess()) return res.chainError(src.loc(i + n), String.format("Expected a value after the '%s' operator.", op.readable));
            n += res.n;

            return ParseRes.res(new OperationStatement(loc, op.operation, prev, res.result), n);
        }

        return ParseRes.failed();
    }
}
