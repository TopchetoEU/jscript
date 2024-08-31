package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.values.FunctionNode;

public class VariableDeclareNode extends Node {
    public static class Pair {
        public final String name;
        public final Node value;
        public final Location location;

        public Pair(String name, Node value, Location location) {
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
                FunctionNode.compileWithName(entry.value, target, true, entry.name, BreakpointType.STEP_OVER);
                target.add(Instruction.storeVar(key));
            }
        }

        if (pollute) target.add(Instruction.pushUndefined());
    }

    public VariableDeclareNode(Location loc, List<Pair> values) {
        super(loc);
        this.values = values;
    }

    public static ParseRes<VariableDeclareNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "var")) return ParseRes.failed();
        n += 3;

        var res = new ArrayList<Pair>();

        var end = JavaScript.parseStatementEnd(src, i + n);
        if (end.isSuccess()) {
            n += end.n;
            return ParseRes.res(new VariableDeclareNode(loc, res), n);
        }

        while (true) {
            var nameLoc = src.loc(i + n);
            var name = Parsing.parseIdentifier(src, i + n);
            if (!name.isSuccess()) return name.chainError(nameLoc, "Expected a variable name");
            n += name.n;

            if (!JavaScript.checkVarName(name.result)) {
                return ParseRes.error(src.loc(i + n), String.format("Unexpected identifier '%s'", name.result));
            }

            Node val = null;
            n += Parsing.skipEmpty(src, i + n);

            if (src.is(i + n, "=")) {
                n++;

                var valRes = JavaScript.parseExpression(src, i + n, 2);
                if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after '='");

                n += valRes.n;
                n += Parsing.skipEmpty(src, i + n);
                val = valRes.result;
            }

            res.add(new Pair(name.result, val, nameLoc));

            if (src.is(i + n, ",")) {
                n++;
                continue;
            }

            end = JavaScript.parseStatementEnd(src, i + n);

            if (end.isSuccess()) {
                n += end.n;
                return ParseRes.res(new VariableDeclareNode(loc, res), n);
            }
            else return end.chainError(src.loc(i + n), "Expected a comma or end of statement");
        }
    }
}