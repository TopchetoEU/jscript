package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
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
import me.topchetoeu.jscript.compilation.VariableDeclareNode;
import me.topchetoeu.jscript.compilation.values.operations.DiscardNode;

public class ForNode extends Node {
    public final Node declaration, assignment, condition, body;
    public final String label;

    @Override public void resolve(CompileResult target) {
        declaration.resolve(target);
        body.resolve(target);
    }
    @Override public void compile(CompileResult target, boolean pollute) {
        declaration.compile(target, false, BreakpointType.STEP_OVER);

        int start = target.size();
        condition.compile(target, true, BreakpointType.STEP_OVER);
        int mid = target.temp();

        var end = new DeferredIntSupplier();
        LabelContext.pushLoop(target.env, loc(), label, end, start);

        var subtarget = target.subtarget();
        subtarget.add(() -> Instruction.stackAlloc(subtarget.scope.allocCount()));

        body.compile(subtarget, false, BreakpointType.STEP_OVER);

        subtarget.scope.end();
        subtarget.add(Instruction.stackFree(subtarget.scope.allocCount()));

        LabelContext.popLoop(target.env, label);

        // int beforeAssign = target.size();
        assignment.compile(target, false, BreakpointType.STEP_OVER);
        int endI = target.size();
        end.set(endI);

        // WhileNode.replaceBreaks(target, label, mid + 1, end, beforeAssign, end + 1);

        target.add(Instruction.jmp(start - endI));
        target.set(mid, Instruction.jmpIfNot(endI - mid + 1));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public ForNode(Location loc, String label, Node declaration, Node condition, Node assignment, Node body) {
        super(loc);
        this.label = label;
        this.declaration = declaration;
        this.condition = condition;
        this.assignment = assignment;
        this.body = body;
    }

    private static ParseRes<Node> parseSemicolon(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        if (!src.is(i + n, ";")) return ParseRes.failed();
        else return ParseRes.res(new DiscardNode(src.loc(i), null), n + 1);
    }
    private static ParseRes<Node> parseCondition(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        var res = JavaScript.parseExpression(src, i + n, 0);
        if (!res.isSuccess()) return res.chainError();
        n += res.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ";")) return ParseRes.error(src.loc(i + n), "Expected a semicolon");
        else return ParseRes.res(res.result, n + 1);
    }
    private static ParseRes<? extends Node> parseUpdater(Source src, int i) {
        return JavaScript.parseExpression(src, i, 0);
    }

    public static ParseRes<ForNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var labelRes = JavaScript.parseLabel(src, i + n);
        n += labelRes.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!Parsing.isIdentifier(src, i + n, "for")) return ParseRes.failed();
        n += 3;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "(")) return ParseRes.error(src.loc(i + n), "Expected a open paren after 'for'");
        n++;

        ParseRes<Node> decl = ParseRes.first(src, i + n,
            ForNode::parseSemicolon,
            VariableDeclareNode::parse,
            ForNode::parseCondition
        );
        if (!decl.isSuccess()) return decl.chainError(src.loc(i + n), "Expected a declaration or an expression");
        n += decl.n;

        ParseRes<Node> cond = ParseRes.first(src, i + n,
            ForNode::parseSemicolon,
            ForNode::parseCondition
        );
        if (!cond.isSuccess()) return cond.chainError(src.loc(i + n), "Expected a condition");
        n += cond.n;

        var update = parseUpdater(src, i + n);
        if (update.isError()) return update.chainError();
        n += update.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a close paren after for updater");
        n++;

        var body = JavaScript.parseStatement(src, i + n);
        if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected a for body.");
        n += body.n;

        return ParseRes.res(new ForNode(loc, labelRes.result, decl.result, cond.result, update.result, body.result), n);
    }
}