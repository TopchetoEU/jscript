package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;

public class DiscardStatement extends Statement {
    public final Statement value;

    @Override public boolean pure() { return value.pure(); }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        value.compile(target, false);
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public DiscardStatement(Location loc, Statement val) {
        super(loc);
        this.value = val;
    }

    public static ParseRes<DiscardStatement> parse(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        var n = 0;
        if (!Parsing.isIdentifier(tokens, i + n++, "void")) return ParseRes.failed();

        var valRes = Parsing.parseValue(filename, tokens, i + n, 14);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'void' keyword.", valRes);
        n += valRes.n;

        return ParseRes.res(new DiscardStatement(loc, valRes.result), n);
    }
}
