package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.common.Filename;
import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.ParseRes;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.parsing.Operator;
import me.topchetoeu.jscript.compilation.parsing.Parsing;
import me.topchetoeu.jscript.compilation.parsing.Token;
import me.topchetoeu.jscript.compilation.values.FunctionStatement;

public class VariableDeclareStatement extends Statement {
    public static class Pair {
        public final String name;
        public final Statement value;
        public final Location location;

        public Pair(String name, Statement value, Location location) {
            this.name = name;
            this.value = value;
            this.location = location;
        }
    }

    public final List<Pair> values;

    @Override
    public void declare(CompileResult target) {
        for (var key : values) {
            target.scope.define(key.name);
        }
    }
    @Override
    public void compile(CompileResult target, boolean pollute) {
        for (var entry : values) {
            if (entry.name == null) continue;
            var key = target.scope.getKey(entry.name);

            if (key instanceof String) target.add(Instruction.makeVar((String)key));

            if (entry.value != null) {
                FunctionStatement.compileWithName(entry.value, target, true, entry.name, BreakpointType.STEP_OVER);
                target.add(Instruction.storeVar(key));
            }
        }

        if (pollute) target.add(Instruction.pushUndefined());
    }

    public VariableDeclareStatement(Location loc, List<Pair> values) {
        super(loc);
        this.values = values;
    }

    public static ParseRes<VariableDeclareStatement> parse(Filename filename, List<Token> tokens, int i) {
        var loc = Parsing.getLoc(filename, tokens, i);
        int n = 0;
        if (!Parsing.isIdentifier(tokens, i + n++, "var")) return ParseRes.failed();

        var res = new ArrayList<Pair>();

        if (Parsing.isStatementEnd(tokens, i + n)) {
            if (Parsing.isOperator(tokens, i + n, Operator.SEMICOLON)) return ParseRes.res(new VariableDeclareStatement(loc, res), 2);
            else return ParseRes.res(new VariableDeclareStatement(loc, res), 1);
        }

        while (true) {
            var nameLoc = Parsing.getLoc(filename, tokens, i + n);
            var nameRes = Parsing.parseIdentifier(tokens, i + n++);
            if (!nameRes.isSuccess()) return ParseRes.error(loc, "Expected a variable name.");

            if (!Parsing.checkVarName(nameRes.result)) {
                return ParseRes.error(loc, String.format("Unexpected identifier '%s'.", nameRes.result));
            }

            Statement val = null;

            if (Parsing.isOperator(tokens, i + n, Operator.ASSIGN)) {
                n++;
                var valRes = Parsing.parseValue(filename, tokens, i + n, 2);
                if (!valRes.isSuccess()) return ParseRes.error(loc, "Expected a value after '='.", valRes);
                n += valRes.n;
                val = valRes.result;
            }

            res.add(new Pair(nameRes.result, val, nameLoc));

            if (Parsing.isOperator(tokens, i + n, Operator.COMMA)) {
                n++;
                continue;
            }
            else if (Parsing.isStatementEnd(tokens, i + n)) {
                if (Parsing.isOperator(tokens, i + n, Operator.SEMICOLON)) return ParseRes.res(new VariableDeclareStatement(loc, res), n + 1);
                else return ParseRes.res(new VariableDeclareStatement(loc, res), n);
            }
            else return ParseRes.error(loc, "Expected a comma or end of statement.");
        }
    }
}
