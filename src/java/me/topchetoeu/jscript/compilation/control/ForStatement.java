package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.ES5;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.VariableDeclareStatement;
import me.topchetoeu.jscript.compilation.values.operations.DiscardStatement;

public class ForStatement extends Statement {
    public final Statement declaration, assignment, condition, body;
    public final String label;

    @Override
    public void declare(CompileResult target) {
        declaration.declare(target);
        body.declare(target);
    }
    @Override
    public void compile(CompileResult target, boolean pollute) {
        declaration.compile(target, false, BreakpointType.STEP_OVER);

        int start = target.size();
        condition.compile(target, true, BreakpointType.STEP_OVER);
        int mid = target.temp();
        body.compile(target, false, BreakpointType.STEP_OVER);
        int beforeAssign = target.size();
        assignment.compile(target, false, BreakpointType.STEP_OVER);
        int end = target.size();

        WhileStatement.replaceBreaks(target, label, mid + 1, end, beforeAssign, end + 1);

        target.add(Instruction.jmp(start - end));
        target.set(mid, Instruction.jmpIfNot(end - mid + 1));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public ForStatement(Location loc, String label, Statement declaration, Statement condition, Statement assignment, Statement body) {
        super(loc);
        this.label = label;
        this.declaration = declaration;
        this.condition = condition;
        this.assignment = assignment;
        this.body = body;
    }

    private static ParseRes<Statement> parseSemicolon(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        if (!src.is(i + n, ";")) return ParseRes.failed();
        else return ParseRes.res(new DiscardStatement(src.loc(i), null), n + 1);
    }
    private static ParseRes<Statement> parseCondition(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);

        var res = ES5.parseExpression(src, i + n, 0);
        if (!res.isSuccess()) return res.chainError();
        n += res.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ";")) return ParseRes.error(src.loc(i + n), "Expected a semicolon");
        else return ParseRes.res(res.result, n + 1);
    }
    private static ParseRes<? extends Statement> parseUpdater(Source src, int i) {
        return ES5.parseExpression(src, i, 0);
    }

    public static ParseRes<ForStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var labelRes = WhileStatement.parseLabel(src, i + n);
        n += labelRes.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!Parsing.isIdentifier(src, i + n, "for")) return ParseRes.failed();
        n += 3;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, "(")) return ParseRes.error(src.loc(i + n), "Expected a open paren after 'for'");
        n++;

        ParseRes<Statement> decl = ParseRes.first(src, i + n,
            ForStatement::parseSemicolon,
            VariableDeclareStatement::parse,
            ForStatement::parseCondition
        );
        if (!decl.isSuccess()) return decl.chainError(src.loc(i + n), "Expected a declaration or an expression");
        n += decl.n;

        ParseRes<Statement> cond = ParseRes.first(src, i + n,
            ForStatement::parseSemicolon,
            ForStatement::parseCondition
        );
        if (!cond.isSuccess()) return cond.chainError(src.loc(i + n), "Expected a condition");
        n += cond.n;

        var update = parseUpdater(src, i + n);
        if (update.isError()) return update.chainError();
        n += update.n;
        n += Parsing.skipEmpty(src, i + n);

        if (!src.is(i + n, ")")) return ParseRes.error(src.loc(i + n), "Expected a close paren after for updater");
        n++;

        var body = ES5.parseStatement(src, i + n);
        if (!body.isSuccess()) return body.chainError(src.loc(i + n), "Expected a for body.");
        n += body.n;

        return ParseRes.res(new ForStatement(loc, labelRes.result, decl.result, cond.result, update.result, body.result), n);
    }
}
