package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

public class BreakStatement extends Statement {
    public final String label;

    @Override public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.nop("break", label));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public BreakStatement(Location loc, String label) {
        super(loc);
        this.label = label;
    }

    public static ParseRes<BreakStatement> parseBreak(Filename filename, List<Token> tokens, int i) {
        if (!Parsing.isIdentifier(tokens, i, "break")) return ParseRes.failed();

        if (Parsing.isStatementEnd(tokens, i + 1)) {
            if (Parsing.isOperator(tokens, i + 1, Operator.SEMICOLON)) return ParseRes.res(new BreakStatement(Parsing.getLoc(filename, tokens, i), null), 2);
            else return ParseRes.res(new BreakStatement(Parsing.getLoc(filename, tokens, i), null), 1);
        }

        var labelRes = Parsing.parseIdentifier(tokens, i + 1);
        if (labelRes.isFailed()) return ParseRes.error(Parsing.getLoc(filename, tokens, i), "Expected a label name or an end of statement.");
        var label = labelRes.result;

        if (Parsing.isStatementEnd(tokens, i + 2)) {
            if (Parsing.isOperator(tokens, i + 2, Operator.SEMICOLON)) return ParseRes.res(new BreakStatement(Parsing.getLoc(filename, tokens, i), label), 3);
            else return ParseRes.res(new BreakStatement(Parsing.getLoc(filename, tokens, i), label), 2);
        }
        else return ParseRes.error(Parsing.getLoc(filename, tokens, i), "Expected an end of statement.");
    }
}
