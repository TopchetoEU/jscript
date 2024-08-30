package me.topchetoeu.jscript.compilation.values;

import java.util.ArrayList;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.ES5;
import me.topchetoeu.jscript.compilation.Statement;

public class ArrayStatement extends Statement {
    public final Statement[] statements;

    @Override public boolean pure() {
        for (var stm : statements) {
            if (!stm.pure()) return false;
        }

        return true;
    }

    @Override public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.loadArr(statements.length));

        for (var i = 0; i < statements.length; i++) {
            var el = statements[i];
            if (el != null) {
                target.add(Instruction.dup());
                target.add(Instruction.pushValue(i));
                el.compile(target, true);
                target.add(Instruction.storeMember());
            }
        }

        if (!pollute) target.add(Instruction.discard());
    }

    public ArrayStatement(Location loc, Statement[] statements) {
        super(loc);
        this.statements = statements;
    }

    public static ParseRes<ArrayStatement> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "[")) return ParseRes.failed();
        n++;

        var values = new ArrayList<Statement>();

        loop: while (true) {
            n += Parsing.skipEmpty(src, i + n);
            if (src.is(i + n, "]")) {
                n++;
                break;
            }

            while (src.is(i + n, ",")) {
                n++;
                n += Parsing.skipEmpty(src, i + n);
                values.add(null);

                if (src.is(i + n, "]")) {
                    n++;
                    break loop;
                }
            }

            var res = ES5.parseExpression(src, i + n, 2);
            if (!res.isSuccess()) return res.chainError(src.loc(i + n), "Expected an array element.");
            n += res.n;
            n += Parsing.skipEmpty(src, i + n);

            values.add(res.result);

            if (src.is(i + n, ",")) n++;
            else if (src.is(i + n, "]")) {
                n++;
                break;
            }
        }

        return ParseRes.res(new ArrayStatement(loc, values.toArray(Statement[]::new)), n);
    }
}
