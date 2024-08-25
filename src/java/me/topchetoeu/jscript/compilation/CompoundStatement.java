package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;
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

    public static ParseRes<CompoundStatement> parseComma(Filename filename, List<Token> tokens, int i, Statement prev, int precedence) {
        var loc = Parsing.getLoc(filename, tokens, i);
        var n = 0;

        if (precedence > 1) return ParseRes.failed();
        if (!Parsing.isOperator(tokens, i + n++, Operator.COMMA)) return ParseRes.failed();

        var res = Parsing.parseValue(filename, tokens, i + n, 2);
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected a value after the comma.", res);
        n += res.n;

        return ParseRes.res(new CompoundStatement(loc, false, prev, res.result), n);
    }
    public static ParseRes<CompoundStatement> parse(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;
        if (!Parsing.isOperator(tokens, i + n++, Operator.BRACE_OPEN)) return ParseRes.failed();

        var statements = new ArrayList<Statement>();

        while (true) {
            if (Parsing.isOperator(tokens, i + n, Operator.BRACE_CLOSE)) {
                n++;
                break;
            }
            if (Parsing.isOperator(tokens, i + n, Operator.SEMICOLON)) {
                n++;
                continue;
            }

            var res = Parsing.parseStatement(filename, tokens, i + n);
            if (!res.isSuccess()) {
                return ParseRes.error(Parsing.getLoc(filename, tokens, i), "Expected a statement.", res);
            }
            n += res.n;

            statements.add(res.result);
        }

        return ParseRes.res(new CompoundStatement(loc, true, statements.toArray(Statement[]::new)).setEnd(Parsing.getLoc(filename, tokens, i + n - 1)), n);
    }
}
