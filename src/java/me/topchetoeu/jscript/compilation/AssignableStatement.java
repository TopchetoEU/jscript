package me.topchetoeu.jscript.compilation;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.common.ParseRes.State;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

public abstract class AssignableStatement extends Statement {
    public abstract Statement toAssign(Statement val, Operation operation);

    protected AssignableStatement(Location loc) {
        super(loc);
    }

    public static ParseRes<? extends Statement> parse(Filename filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;

        if (precedence > 2) return ParseRes.failed();

        var opRes = Parsing.parseOperator(tokens, i + n++);
        if (opRes.state != State.SUCCESS) return ParseRes.failed();

        var op = opRes.result;
        if (!op.isAssign()) return ParseRes.failed();

        if (!(prev instanceof AssignableStatement)) {
            return ParseRes.error(loc, "Invalid expression on left hand side of assign operator.");
        }

        var res = Parsing.parseValue(filename, tokens, i + n, 2);
        if (!res.isSuccess()) return ParseRes.error(loc, String.format("Expected value after assignment operator '%s'.", op.readable), res);
        n += res.n;

        Operation operation = null;

        if (op == Operator.ASSIGN_ADD) operation = Operation.ADD;
        if (op == Operator.ASSIGN_SUBTRACT) operation = Operation.SUBTRACT;
        if (op == Operator.ASSIGN_MULTIPLY) operation = Operation.MULTIPLY;
        if (op == Operator.ASSIGN_DIVIDE) operation = Operation.DIVIDE;
        if (op == Operator.ASSIGN_MODULO) operation = Operation.MODULO;
        if (op == Operator.ASSIGN_OR) operation = Operation.OR;
        if (op == Operator.ASSIGN_XOR) operation = Operation.XOR;
        if (op == Operator.ASSIGN_AND) operation = Operation.AND;
        if (op == Operator.ASSIGN_SHIFT_LEFT) operation = Operation.SHIFT_LEFT;
        if (op == Operator.ASSIGN_SHIFT_RIGHT) operation = Operation.SHIFT_RIGHT;
        if (op == Operator.ASSIGN_USHIFT_RIGHT) operation = Operation.USHIFT_RIGHT;

        return ParseRes.res(((AssignableStatement)prev).toAssign(res.result, operation), n);
    }

}
