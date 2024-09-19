package me.topchetoeu.jscript.compilation.control;

import java.util.ArrayList;
import java.util.HashMap;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.DeferredIntSupplier;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.LabelContext;
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
    public final String label;

    @Override public void resolve(CompileResult target) {
        for (var stm : body) stm.resolve(target);
    }

    @Override public void compile(CompileResult target, boolean pollute) {
        var caseToStatement = new HashMap<Integer, Integer>();
        var statementToIndex = new HashMap<Integer, Integer>();

        value.compile(target, true, BreakpointType.STEP_OVER);

        var subtarget = target.subtarget();
        subtarget.beginScope();

        // TODO: create a jump map
        for (var ccase : cases) {
            subtarget.add(Instruction.dup());
            ccase.value.compile(subtarget, true);
            subtarget.add(Instruction.operation(Operation.EQUALS));
            caseToStatement.put(subtarget.temp(), ccase.statementI);
        }

        int start = subtarget.temp();
        var end = new DeferredIntSupplier();

        LabelContext.getBreak(target.env).push(loc(), label, end);
        for (var stm : body) {
            statementToIndex.put(statementToIndex.size(), subtarget.size());
            stm.compile(subtarget, false, BreakpointType.STEP_OVER);
        }
        LabelContext.getBreak(target.env).pop(label);

        subtarget.endScope();

        int endI = subtarget.size();
        end.set(endI);
        subtarget.add(Instruction.discard());
        if (pollute) subtarget.add(Instruction.pushUndefined());

        if (defaultI < 0 || defaultI >= body.length) subtarget.set(start, Instruction.jmp(endI - start));
        else subtarget.set(start, Instruction.jmp(statementToIndex.get(defaultI) - start));

        for (var el : caseToStatement.entrySet()) {
            var i = statementToIndex.get(el.getValue());
            if (i == null) i = endI;
            subtarget.set(el.getKey(), Instruction.jmpIf(i - el.getKey()));
        }

    }

    public SwitchNode(Location loc, String label, Node value, int defaultI, SwitchCase[] cases, Node[] body) {
        super(loc);
        this.label = label;
        this.value = value;
        this.defaultI = defaultI;
        this.cases = cases;
        this.body = body;
    }

    private static ParseRes<Node> parseSwitchCase(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        if (!Parsing.isIdentifier(src, i + n, "case")) return ParseRes.failed();
        n += 4;

        var val = JavaScript.parseExpression(src, i + n, 0);
        if (!val.isSuccess()) return val.chainError(src.loc(i + n), "Expected a value after 'case'");
        n += val.n;

        if (!src.is(i + n, ":")) return ParseRes.error(src.loc(i + n), "Expected colons after 'case' value");
        n++;

        return ParseRes.res(val.result, n);
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

        var label = JavaScript.parseLabel(src, i + n);
        n += label.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!Parsing.isIdentifier(src, i + n, "switch")) return ParseRes.failed();
        n += 6;
        n += Parsing.skipEmpty(src, i + n);
        if (!src.is(i + n, "(")) return ParseRes.error(src.loc(i + n), "Expected a open paren after 'switch'");
        n++;

        var val = JavaScript.parseExpression(src, i + n, 0);
        if (!val.isSuccess()) return val.chainError(src.loc(i + n), "Expected a switch value");
        n += val.n;
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
            loc, label.result, val.result, defaultI,
            cases.toArray(new SwitchCase[0]),
            statements.toArray(new Node[0])
        ), n);
    }
}
