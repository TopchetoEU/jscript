package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.VariableDeclareStatement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;
import me.topchetoeu.jscript.compilation.values.ConstantStatement;

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

    public static ParseRes<ForStatement> parse(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;

        var labelRes = WhileStatement.parseLabel(tokens, i + n);
        n += labelRes.n;

        if (!Parsing.isIdentifier(tokens, i + n++, "for")) return ParseRes.failed();
        if (!Parsing.isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a open paren after 'for'.");

        Statement decl, cond, inc;

        if (Parsing.isOperator(tokens, i + n, Operator.SEMICOLON)) {
            n++;
            decl = new CompoundStatement(loc, false);
        }
        else {
            var declRes = ParseRes.any(
                VariableDeclareStatement.parse(filename, tokens, i + n),
                Parsing.parseValueStatement(filename, tokens, i + n)
            );
            if (!declRes.isSuccess()) return ParseRes.error(loc, "Expected a declaration or an expression.", declRes);
            n += declRes.n;
            decl = declRes.result;
        }

        if (Parsing.isOperator(tokens, i + n, Operator.SEMICOLON)) {
            n++;
            cond = new ConstantStatement(loc, 1);
        }
        else {
            var condRes = Parsing.parseValue(filename, tokens, i + n, 0);
            if (!condRes.isSuccess()) return ParseRes.error(loc, "Expected a condition.", condRes);
            n += condRes.n;
            if (!Parsing.isOperator(tokens, i + n++, Operator.SEMICOLON)) return ParseRes.error(loc, "Expected a semicolon.", condRes);
            cond = condRes.result;
        }

        if (Parsing.isOperator(tokens, i + n, Operator.PAREN_CLOSE)) {
            n++;
            inc = new CompoundStatement(loc, false);
        }
        else {
            var incRes = Parsing.parseValue(filename, tokens, i + n, 0);
            if (!incRes.isSuccess()) return ParseRes.error(loc, "Expected a condition.", incRes);
            n += incRes.n;
            inc = incRes.result;
            if (!Parsing.isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.error(loc, "Expected a closing paren after for.");
        }
        

        var res = Parsing.parseStatement(filename, tokens, i + n);
        if (!res.isSuccess()) return ParseRes.error(loc, "Expected a for body.", res);
        n += res.n;

        return ParseRes.res(new ForStatement(loc, labelRes.result, decl, cond, inc, res.result), n);
    }
}
