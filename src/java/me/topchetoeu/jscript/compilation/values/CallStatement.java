package me.topchetoeu.jscript.compilation.values;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

public class CallStatement extends Statement {
    public final Statement func;
    public final Statement[] args;
    public final boolean isNew;

    @Override public void compile(CompileResult target, boolean pollute, BreakpointType type) {
        if (isNew) func.compile(target, true);
        else if (func instanceof IndexStatement) {
            ((IndexStatement)func).compile(target, true, true);
        }
        else {
            target.add(Instruction.pushUndefined());
            func.compile(target, true);
        }

        for (var arg : args) arg.compile(target, true);

        if (isNew) target.add(Instruction.callNew(args.length)).setLocationAndDebug(loc(), type);
        else target.add(Instruction.call(args.length)).setLocationAndDebug(loc(), type);

        if (!pollute) target.add(Instruction.discard());
    }
    @Override public void compile(CompileResult target, boolean pollute) {
        compile(target, pollute, BreakpointType.STEP_IN);
    }

    public CallStatement(Location loc, boolean isNew, Statement func, Statement ...args) {
        super(loc);
        this.isNew = isNew;
        this.func = func;
        this.args = args;
    }

    public static ParseRes<CallStatement> parseCall(Filename filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = Parsing.getLoc(filename, tokens, i);
        var n = 0;

        if (precedence > 17) return ParseRes.failed();
        if (!Parsing.isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.failed();

        var args = new ArrayList<Statement>();
        boolean prevArg = false;

        while (true) {
            var argRes = Parsing.parseValue(filename, tokens, i + n, 2);
            if (argRes.isSuccess()) {
                args.add(argRes.result);
                n += argRes.n;
                prevArg = true;
            }
            else if (argRes.isError()) return argRes.transform();
            else if (prevArg && Parsing.isOperator(tokens, i + n, Operator.COMMA)) {
                prevArg = false;
                n++;
            }
            else if (Parsing.isOperator(tokens, i + n, Operator.PAREN_CLOSE)) {
                n++;
                break;
            }
            else return ParseRes.error(Parsing.getLoc(filename, tokens, i + n), prevArg ? "Expected a comma or a closing paren." : "Expected an expression or a closing paren.");
        }

        return ParseRes.res(new CallStatement(loc, false, prev, args.toArray(Statement[]::new)), n);
    }
    public static ParseRes<CallStatement> parseNew(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        var n = 0;
        if (!Parsing.isIdentifier(tokens, i + n++, "new")) return ParseRes.failed();

        var valRes = Parsing.parseValue(filename, tokens, i + n, 18);
        n += valRes.n;
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'new' keyword.", valRes);
        var callRes = CallStatement.parseCall(filename, tokens, i + n, valRes.result, 0);
        n += callRes.n;
        if (callRes.isError()) return callRes.transform();
        else if (callRes.isFailed()) return ParseRes.res(new CallStatement(loc, true, valRes.result), n);
        var call = (CallStatement)callRes.result;

        return ParseRes.res(new CallStatement(loc, true, call.func, call.args), n);
    }
}
