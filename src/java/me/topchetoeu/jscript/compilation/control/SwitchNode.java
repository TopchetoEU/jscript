package me.topchetoeu.jscript.compilation.control;

import java.util.ArrayList;
import java.util.HashMap;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.Instruction.Type;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;

public class SwitchNode extends Node {
    public static class SwitchCase {
        public final Node value;
        public final int statementI;

        public SwitchCase(Node value, int statementI) {
            this.value = value;
            this.statementI = statementI;
        }
    }

    public final Node value;
    public final SwitchCase[] cases;
    public final Node[] body;
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

    public SwitchNode(Location loc, Node value, int defaultI, SwitchCase[] cases, Node[] body) {
        super(loc);
        this.value = value;
        this.defaultI = defaultI;
        this.cases = cases;
        this.body = body;
    }

    private static ParseRes<Node> parseSwitchCase(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        if (!Parsing.isIdentifier(src, i + n, "case")) return ParseRes.failed();
        n += 4;

        var valRes = JavaScript.parseExpression(src, i + n, 0);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'case'");
        n += valRes.n;

        if (!src.is(i + n, ":")) return ParseRes.error(src.loc(i + n), "Expected colons after 'case' value");
        n++;

        return ParseRes.res(valRes.result, n);
    }
    private static ParseRes<Void> parseDefaultCase(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        if (!Parsing.isIdentifier(src, i + n, "default")) return ParseRes.failed();
        n += 7;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ":")) return ParseRes.error(src.loc(i + n), "Expected colons after 'default'");
        n++;

        return ParseRes.res(null, n);
    }
    @SuppressWarnings("unused")
    public static ParseRes<SwitchNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "switch")) return ParseRes.failed();
        n += 6;
        n += Parsing.skipEmpty(src, i + n);
        if (!src.is(i + n, "(")) return ParseRes.error(src.loc(i + n), "Expected a open paren after 'switch'");
        n++;

        var valRes = JavaScript.parseExpression(src, i + n, 0);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a switch value");
        n += valRes.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a closing paren after switch value");
        n++;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "{")) return ParseRes.error(src.loc(i + n), "Expected an opening brace after switch value");
        n++;
        n += Parsing.skipEmpty(src, i + n);

        var statements = new ArrayList<Node>();
        var cases = new ArrayList<SwitchCase>();
        var defaultI = -1;

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

            ParseRes<Node> caseRes = ParseRes.first(src, i + n,
                SwitchNode::parseDefaultCase,
                SwitchNode::parseSwitchCase
            );

            // Parsing::parseStatement

            if (caseRes.isSuccess()) {
                n += caseRes.n;

                if (caseRes.result == null) defaultI = statements.size();
                else cases.add(new SwitchCase(caseRes.result, statements.size()));
                continue;
            }
            if (caseRes.isError()) return caseRes.chainError();

            var stm = JavaScript.parseStatement(src, i + n);
            if (stm.isSuccess()) {
                n += stm.n;
                statements.add(stm.result);
                continue;
            }
            else stm.chainError(src.loc(i + n), "Expected a statement, 'case' or 'default'");
        }

        return ParseRes.res(new SwitchNode(
            loc, valRes.result, defaultI,
            cases.toArray(SwitchCase[]::new),
            statements.toArray(Node[]::new)
        ), n);
    }
}
