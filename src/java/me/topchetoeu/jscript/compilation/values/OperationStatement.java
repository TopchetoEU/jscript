package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

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

    public static ParseRes<OperationStatement> parseUnary(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;

        var opState = Parsing.parseOperator(tokens, i + n++);
        if (!opState.isSuccess()) return ParseRes.failed();
        var op = opState.result;

        Operation operation = null;

        if (op == Operator.ADD) operation = Operation.POS;
        else if (op == Operator.SUBTRACT) operation = Operation.NEG;
        else if (op == Operator.INVERSE) operation = Operation.INVERSE;
        else if (op == Operator.NOT) operation = Operation.NOT;
        else return ParseRes.failed();

        var res = Parsing.parseValue(filename, tokens, n + i, 14);

        if (res.isSuccess()) return ParseRes.res(new OperationStatement(loc, operation, res.result), n + res.n);
        else return ParseRes.error(loc, String.format("Expected a value after the unary operator '%s'.", op.readable), res);
    }
    public static ParseRes<OperationStatement> parseInstanceof(Filename filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;

        if (precedence > 9) return ParseRes.failed();
        if (!Parsing.isIdentifier(tokens, i + n++, "instanceof")) return ParseRes.failed();

        var valRes = Parsing.parseValue(filename, tokens, i + n, 10);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'instanceof'.", valRes);
        n += valRes.n;

        return ParseRes.res(new OperationStatement(loc, Operation.INSTANCEOF, prev, valRes.result), n);
    }
    public static ParseRes<OperationStatement> parseIn(Filename filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;

        if (precedence > 9) return ParseRes.failed();
        if (!Parsing.isIdentifier(tokens, i + n++, "in")) return ParseRes.failed();

        var valRes = Parsing.parseValue(filename, tokens, i + n, 10);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'in'.", valRes);
        n += valRes.n;

        return ParseRes.res(new OperationStatement(loc, Operation.IN, prev, valRes.result), n);
    }
    public static ParseRes<? extends Statement> parseOperator(Filename filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = Parsing.getLoc(filename, tokens, i);
        var n = 0;

        var opRes = Parsing.parseOperator(tokens, i + n++);
        if (!opRes.isSuccess()) return ParseRes.failed();
        var op = opRes.result;

        if (op.precedence < precedence) return ParseRes.failed();
        if (op.isAssign()) return AssignableStatement.parse(filename, tokens, i + n - 1, prev, precedence);

        var res = Parsing.parseValue(filename, tokens, i + n, op.precedence + (op.reverse ? 0 : 1));
        if (!res.isSuccess()) return ParseRes.error(loc, String.format("Expected a value after the '%s' operator.", op.readable), res);
        n += res.n;

        if (op == Operator.LAZY_AND) {
            return ParseRes.res(new LazyAndStatement(loc, prev, res.result), n);
        }
        if (op == Operator.LAZY_OR) {
            return ParseRes.res(new LazyOrStatement(loc, prev, res.result), n);
        }

        return ParseRes.res(new OperationStatement(loc, op.operation, prev, res.result), n);
    }
}
