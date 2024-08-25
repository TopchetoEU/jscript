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

public class DebugStatement extends Statement {
    @Override public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.debug());
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public DebugStatement(Location loc) {
        super(loc);
    }

    public static ParseRes<DebugStatement> parse(Filename filename, List<Token> tokens, int i) {
        if (!Parsing.isIdentifier(tokens, i, "debugger")) return ParseRes.failed();

        if (Parsing.isStatementEnd(tokens, i + 1)) {
            if (Parsing.isOperator(tokens, i + 1, Operator.SEMICOLON)) return ParseRes.res(new DebugStatement(Parsing.getLoc(filename, tokens, i)), 2);
            else return ParseRes.res(new DebugStatement(Parsing.getLoc(filename, tokens, i)), 1);
        }
        else return ParseRes.error(Parsing.getLoc(filename, tokens, i), "Expected an end of statement.");
    }

}
