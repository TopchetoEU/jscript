package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.ES5;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.values.VariableStatement;
import me.topchetoeu.jscript.compilation.values.constants.BoolStatement;
import me.topchetoeu.jscript.compilation.values.operations.IndexStatement;

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

    public static ParseRes<? extends Statement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "delete")) return ParseRes.failed();
        n += 6;

        var valRes = ES5.parseExpression(src, i + n, 15);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'delete'");
        n += valRes.n;

        if (valRes.result instanceof IndexStatement) {
            var index = (IndexStatement)valRes.result;
            return ParseRes.res(new DeleteStatement(loc, index.index, index.object), n);
        }
        else if (valRes.result instanceof VariableStatement) {
            return ParseRes.error(src.loc(i + n), "A variable may not be deleted");
        }
        else return ParseRes.res(new BoolStatement(loc, true), n);
    }

    public DeleteStatement(Location loc, Statement key, Statement value) {
        super(loc);
        this.key = key;
        this.value = value;
    }
}
