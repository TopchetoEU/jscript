package me.topchetoeu.jscript.compilation.values;

import java.util.ArrayList;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.ParseRes;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Source;

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

    public static ParseRes<CallStatement> parseCall(Source src, int i, Statement prev, int precedence) {
        if (precedence > 17) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "(")) return ParseRes.failed();
        n++;

        var args = new ArrayList<Statement>();
        boolean prevArg = false;

        while (true) {
            var argRes = Parsing.parseValue(src, i + n, 2);
            n += argRes.n;
            n += Parsing.skipEmpty(src, i + n);

            if (argRes.isSuccess()) {
                args.add(argRes.result);
                prevArg = true;
            }
            else if (argRes.isError()) return argRes.chainError();
            else if (prevArg && src.is(i + n, ",")) {
                prevArg = false;
                n++;
            }
            else if (src.is(i + n, ")")) {
                n++;
                break;
            }
            else if (prevArg) return ParseRes.error(src.loc(i + n), "Expected a comma or a closing paren");
            else return ParseRes.error(src.loc(i + n), "Expected an expression or a closing paren");
        }

        return ParseRes.res(new CallStatement(loc, false, prev, args.toArray(Statement[]::new)), n);
    }
    public static ParseRes<CallStatement> parseNew(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "new")) return ParseRes.failed();
        n += 3;

        var valRes = Parsing.parseValue(src, i + n, 18);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'new' keyword.");
        n += valRes.n;

        var callRes = CallStatement.parseCall(src, i + n, valRes.result, 0);
        if (callRes.isFailed()) return ParseRes.res(new CallStatement(loc, true, valRes.result), n);
        if (callRes.isError()) return callRes.chainError();
        n += callRes.n;

        return ParseRes.res(new CallStatement(loc, true, callRes.result.func, callRes.result.args), n);
    }
}
