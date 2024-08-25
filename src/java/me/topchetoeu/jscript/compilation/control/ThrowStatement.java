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

public class ThrowStatement extends Statement {
    public final Statement value;

    @Override
    public void compile(CompileResult target, boolean pollute) {
        value.compile(target, true);
        target.add(Instruction.throwInstr()).setLocation(loc());
    }

    public ThrowStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }

    public static ParseRes<ThrowStatement> parseThrow(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;
        if (!Parsing.isIdentifier(tokens, i + n++, "throw")) return ParseRes.failed();

        var valRes = Parsing.parseValue(filename, tokens, i + n, 0);
        n += valRes.n;
        if (valRes.isError()) return ParseRes.error(loc, "Expected a throw value.", valRes);

        var res = ParseRes.res(new ThrowStatement(loc, valRes.result), n);

        if (Parsing.isStatementEnd(tokens, i + n)) {
            if (Parsing.isOperator(tokens, i + n, Operator.SEMICOLON)) return res.addN(1);
            else return res;
        }
        else return ParseRes.error(Parsing.getLoc(filename, tokens, i), "Expected an end of statement.", valRes);
    }
}
