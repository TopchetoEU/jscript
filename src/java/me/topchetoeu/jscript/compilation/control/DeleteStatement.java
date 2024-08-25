package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;
import me.topchetoeu.jscript.compilation.values.ConstantStatement;
import me.topchetoeu.jscript.compilation.values.IndexStatement;
import me.topchetoeu.jscript.compilation.values.VariableStatement;

public class DeleteStatement extends Statement {
    public final Statement key;
    public final Statement value;

    @Override
    public void compile(CompileResult target, boolean pollute) {
        value.compile(target, true);
        key.compile(target, true);

        target.add(Instruction.delete());
        if (pollute) target.add(Instruction.pushValue(true));
    }

    public static ParseRes<? extends Statement> parseDelete(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;
        if (!Parsing.isIdentifier(tokens, i + n++, "delete")) return ParseRes.failed();

        var valRes = Parsing.parseValue(filename, tokens, i + n, 15);
        if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after 'delete'.", valRes);
        n += valRes.n;

        if (valRes.result instanceof IndexStatement) {
            var index = (IndexStatement)valRes.result;
            return ParseRes.res(new DeleteStatement(loc, index.index, index.object), n);
        }
        else if (valRes.result instanceof VariableStatement) {
            return ParseRes.error(loc, "A variable may not be deleted.");
        }
        else return ParseRes.res(new ConstantStatement(loc, true), n);
    }

    public DeleteStatement(Location loc, Statement key, Statement value) {
        super(loc);
        this.key = key;
        this.value = value;
    }
}
