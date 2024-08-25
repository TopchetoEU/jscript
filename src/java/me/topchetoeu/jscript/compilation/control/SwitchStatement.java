package me.topchetoeu.jscript.compilation.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.Instruction.Type;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

public class SwitchStatement extends Statement {
    public static class SwitchCase {
        public final Statement value;
        public final int statementI;

        public SwitchCase(Statement value, int statementI) {
            this.value = value;
            this.statementI = statementI;
        }
    }

    public final Statement value;
    public final SwitchCase[] cases;
    public final Statement[] body;
    public final int defaultI;

    @Override public void declare(CompileResult target) {
        for (var stm : body) stm.declare(target);
    }

    @Override public void compile(CompileResult target, boolean pollute) {
        var caseToStatement = new HashMap<Integer, Integer>();
        var statementToIndex = new HashMap<Integer, Integer>();

        value.compile(target, true, BreakpointType.STEP_OVER);

        for (var ccase : cases) {
            target.add(Instruction.dup());
            ccase.value.compile(target, true);
            target.add(Instruction.operation(Operation.EQUALS));
            caseToStatement.put(target.temp(), ccase.statementI);
        }

        int start = target.temp();

        for (var stm : body) {
            statementToIndex.put(statementToIndex.size(), target.size());
            stm.compile(target, false, BreakpointType.STEP_OVER);
        }

        int end = target.size();
        target.add(Instruction.discard());
        if (pollute) target.add(Instruction.pushUndefined());

        if (defaultI < 0 || defaultI >= body.length) target.set(start, Instruction.jmp(end - start));
        else target.set(start, Instruction.jmp(statementToIndex.get(defaultI) - start));

        for (int i = start; i < end; i++) {
            var instr = target.get(i);
            if (instr.type == Type.NOP && instr.is(0, "break") && instr.get(1) == null) {
                target.set(i, Instruction.jmp(end - i));
            }
        }
        for (var el : caseToStatement.entrySet()) {
            var i = statementToIndex.get(el.getValue());
            if (i == null) i = end;
            target.set(el.getKey(), Instruction.jmpIf(i - el.getKey()));
        }

    }

    public SwitchStatement(Location loc, Statement value, int defaultI, SwitchCase[] cases, Statement[] body) {
        super(loc);
        this.value = value;
        this.defaultI = defaultI;
        this.cases = cases;
        this.body = body;
    }

    private static ParseRes<Statement> parseSwitchCase(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;

        if (!Parsing.isIdentifier(tokens, i + n++, "case")) return ParseRes.failed();

        var valRes = Parsing.parseValue(filename, tokens, i + n, 0);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'case'.", valRes);
        n += valRes.n;

        if (!Parsing.isOperator(tokens, i + n++, Operator.COLON)) return ParseRes.error(loc, "Expected colons after 'case' value.");

        return ParseRes.res(valRes.result, n);
    }
    private static ParseRes<Statement> parseDefaultCase(List<Token> tokens, int i) {
        if (!Parsing.isIdentifier(tokens, i, "default")) return ParseRes.failed();
        if (!Parsing.isOperator(tokens, i + 1, Operator.COLON)) return ParseRes.error(Parsing.getLoc(null, tokens, i), "Expected colons after 'default'.");

        return ParseRes.res(null, 2);
    }
    public static ParseRes<SwitchStatement> parse(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;

        if (!Parsing.isIdentifier(tokens, i + n++, "switch")) return ParseRes.failed();
        if (!Parsing.isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a open paren after 'switch'.");

        var valRes = Parsing.parseValue(filename, tokens, i + n, 0);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a switch value.", valRes);
        n += valRes.n;

        if (!Parsing.isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.error(loc, "Expected a closing paren after switch value.");
        if (!Parsing.isOperator(tokens, i + n++, Operator.BRACE_OPEN)) return ParseRes.error(loc, "Expected an opening brace after switch value.");

        var statements = new ArrayList<Statement>();
        var cases = new ArrayList<SwitchCase>();
        var defaultI = -1;

        while (true) {
            if (Parsing.isOperator(tokens, i + n, Operator.BRACE_CLOSE)) {
                n++;
                break;
            }
            if (Parsing.isOperator(tokens, i + n, Operator.SEMICOLON)) {
                n++;
                continue;
            }

            var defaultRes = SwitchStatement.parseDefaultCase(tokens, i + n);
            var caseRes = SwitchStatement.parseSwitchCase(filename, tokens, i + n);

            if (defaultRes.isSuccess()) {
                defaultI = statements.size();
                n += defaultRes.n;
            }
            else if (caseRes.isSuccess()) {
                cases.add(new SwitchCase(caseRes.result, statements.size()));
                n += caseRes.n;
            }
            else if (defaultRes.isError()) return defaultRes.transform();
            else if (caseRes.isError()) return defaultRes.transform();
            else {
                var res = ParseRes.any(
                    Parsing.parseStatement(filename, tokens, i + n),
                    CompoundStatement.parse(filename, tokens, i + n)
                );
                if (!res.isSuccess()) {
                    return ParseRes.error(Parsing.getLoc(filename, tokens, i), "Expected a statement.", res);
                }
                n += res.n;
                statements.add(res.result);
            }
        }

        return ParseRes.res(new SwitchStatement(
            loc, valRes.result, defaultI,
            cases.toArray(SwitchCase[]::new),
            statements.toArray(Statement[]::new)
        ), n);
    }
}
