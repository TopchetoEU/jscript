package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

public class DoWhileStatement extends Statement {
    public final Statement condition, body;
    public final String label;

    @Override
    public void declare(CompileResult target) {
        body.declare(target);
    }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        int start = target.size();
        body.compile(target, false, BreakpointType.STEP_OVER);
        int mid = target.size();
        condition.compile(target, true, BreakpointType.STEP_OVER);
        int end = target.size();

        WhileStatement.replaceBreaks(target, label, start, mid - 1, mid, end + 1);
        target.add(Instruction.jmpIf(start - end));
    }

    public DoWhileStatement(Location loc, String label, Statement condition, Statement body) {
        super(loc);
        this.label = label;
        this.condition = condition;
        this.body = body;
    }

    public static ParseRes<DoWhileStatement> parseDoWhile(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;

        var labelRes = WhileStatement.parseLabel(tokens, i + n);
        n += labelRes.n;

        if (!Parsing.isIdentifier(tokens, i + n++, "do")) return ParseRes.failed();
        var bodyRes = Parsing.parseStatement(filename, tokens, i + n);
        if (!bodyRes.isSuccess()) return ParseRes.error(loc, "Expected a do-while body.", bodyRes);
        n += bodyRes.n;

        if (!Parsing.isIdentifier(tokens, i + n++, "while")) return ParseRes.error(loc, "Expected 'while' keyword.");
        if (!Parsing.isOperator(tokens, i + n++, Operator.PAREN_OPEN)) return ParseRes.error(loc, "Expected a open paren after 'while'.");

        var condRes = Parsing.parseValue(filename, tokens, i + n, 0);
        if (!condRes.isSuccess()) return ParseRes.error(loc, "Expected a while condition.", condRes);
        n += condRes.n;

        if (!Parsing.isOperator(tokens, i + n++, Operator.PAREN_CLOSE)) return ParseRes.error(loc, "Expected a closing paren after while condition.");

        var res = ParseRes.res(new DoWhileStatement(loc, labelRes.result, condRes.result, bodyRes.result), n);

        if (Parsing.isStatementEnd(tokens, i + n)) {
            if (Parsing.isOperator(tokens, i + n, Operator.SEMICOLON)) return res.addN(1);
            else return res;
        }
        else return ParseRes.error(Parsing.getLoc(filename, tokens, i), "Expected a semicolon.");
    }

}
