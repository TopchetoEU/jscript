package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;


public class CompoundNode extends Node {
    public final Node[] statements;
    public Location end;

    @Override public void resolve(CompileResult target) {
        for (var stm : statements) stm.resolve(target);
    }

    public void compile(CompileResult target, boolean pollute, boolean singleEntry, BreakpointType type) {
        List<Node> statements = new ArrayList<Node>();

        for (var stm : this.statements) {
            if (stm instanceof FunctionStatementNode func) {
                func.compile(target, false);
            }
            else statements.add(stm);
        }

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

    @Override public void compile(CompileResult target, boolean pollute, BreakpointType type) {
        compile(target, pollute, true, type);
    }

    public CompoundNode setEnd(Location loc) {
        this.end = loc;
        return this;
    }

    public CompoundNode(Location loc, Node ...statements) {
        super(loc);
        this.statements = statements;
    }

    public static void compileMultiEntry(Node node, CompileResult target, boolean pollute, BreakpointType type) {
        if (node instanceof CompoundNode comp) {
            comp.compile(target, pollute, false, type);
        }
        else {
            node.compile(target, pollute, type);
        }
    }

    public static ParseRes<CompoundNode> parseComma(Source src, int i, Node prev, int precedence) {
        if (precedence > 1) return ParseRes.failed();

        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, ",")) return ParseRes.failed();
        n++;

        var curr = JavaScript.parseExpression(src, i + n, 2);
        if (!curr.isSuccess()) return curr.chainError(src.loc(i + n), "Expected a value after the comma");
        n += curr.n;

        if (prev instanceof CompoundNode comp) {
            var children = new ArrayList<Node>();
            children.addAll(Arrays.asList(comp.statements));
            children.add(curr.result);

            return ParseRes.res(new CompoundNode(loc, children.toArray(new Node[0])), n);
        }
        else return ParseRes.res(new CompoundNode(loc, prev, curr.result), n);
    }
    public static ParseRes<CompoundNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "{")) return ParseRes.failed();
        n++;

        var statements = new ArrayList<Node>();

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

            var res = JavaScript.parseStatement(src, i + n);
            if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected a statement");
            n += res.n;

            statements.add(res.result);
        }

        return ParseRes.res(new CompoundNode(loc, statements.toArray(new Node[0])).setEnd(src.loc(i + n - 1)), n);
    }
}
