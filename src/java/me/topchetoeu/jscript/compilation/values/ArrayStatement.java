package me.topchetoeu.jscript.compilation.values;

import java.util.ArrayList;
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

public class ArrayStatement extends Statement {
    public final Statement[] statements;

    @Override public boolean pure() {
        for (var stm : statements) {
            if (!stm.pure()) return false;
        }

        return true;
    }

    @Override
    public void compile(CompileResult target, boolean pollute) {
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

    public static ParseRes<ArrayStatement> parse(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;
        if (!Parsing.isOperator(tokens, i + n++, Operator.BRACKET_OPEN)) return ParseRes.failed();

        var values = new ArrayList<Statement>();

        loop: while (true) {
            if (Parsing.isOperator(tokens, i + n, Operator.BRACKET_CLOSE)) {
                n++;
                break;
            }

            while (Parsing.isOperator(tokens, i + n, Operator.COMMA)) {
                n++;
                values.add(null);
                if (Parsing.isOperator(tokens, i + n, Operator.BRACKET_CLOSE)) {
                    n++;
                    break loop;
                }
            }

            var res = Parsing.parseValue(filename, tokens, i + n, 2);
            if (!res.isSuccess()) return ParseRes.error(loc, "Expected an array element.", res);
            else n += res.n;

            values.add(res.result);

            if (Parsing.isOperator(tokens, i + n, Operator.COMMA)) n++;
            else if (Parsing.isOperator(tokens, i + n, Operator.BRACKET_CLOSE)) {
                n++;
                break;
            }
        }

        return ParseRes.res(new ArrayStatement(loc, values.toArray(Statement[]::new)), n);
    }
}
