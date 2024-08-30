package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.values.FunctionStatement;

public class CompoundStatement extends Statement {
    public final Statement[] statements;
    public final boolean separateFuncs;
    public Location end;

    @Override public boolean pure() {
        for (var stm : statements) {
            if (!stm.pure()) return false;
        }

        return true;
    }

    @Override
    public void declare(CompileResult target) {
        for (var stm : statements) stm.declare(target);
    }

    @Override
    public void compile(CompileResult target, boolean pollute, BreakpointType type) {
        List<Statement> statements = new Vector<Statement>();
        if (separateFuncs) for (var stm : this.statements) {
            if (stm instanceof FunctionStatement && ((FunctionStatement)stm).statement) {
                stm.compile(target, false);
            }
            else statements.add(stm);
        }
        else statements = List.of(this.statements);

        var polluted = false;

        for (var i = 0; i < statements.size(); i++) {
            var stm = statements.get(i);

            if (i != statements.size() - 1) stm.compile(target, false, BreakpointType.STEP_OVER);
            else stm.compile(target, polluted = pollute, BreakpointType.STEP_OVER);
        }

        if (!polluted && pollute) {
            target.add(Instruction.pushUndefined());
        }
    }

    public CompoundStatement setEnd(Location loc) {
        this.end = loc;
        return this;
    }

    public CompoundStatement(Location loc, boolean separateFuncs, Statement ...statements) {
        super(loc);
        this.separateFuncs = separateFuncs;
        this.statements = statements;
    }

    public static ParseRes<CompoundStatement> parseComma(Source src, int i, Statement prev, int precedence) {
        if (precedence > 1) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, ",")) return ParseRes.failed();
        n++;

        var res = ES5.parseExpression(src, i + n, 2);
        if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected a value after the comma");
        n += res.n;

        return ParseRes.res(new CompoundStatement(loc, false, prev, res.result), n);
    }
    public static ParseRes<CompoundStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "{")) return ParseRes.failed();
        n++;

        var statements = new ArrayList<Statement>();

        while (true) {
            n += Parsing.skipEmpty(src, i + n);

            if (src.is(i + n, "}")) {
                n++;
                break;
            }
            if (src.is(i + n, ";")) {
                n++;
                continue;
            }

            var res = ES5.parseStatement(src, i + n);
            if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected a statement");
            n += res.n;

            statements.add(res.result);
        }

        return ParseRes.res(new CompoundStatement(loc, true, statements.toArray(Statement[]::new)).setEnd(src.loc(i + n - 1)), n);
    }
}
