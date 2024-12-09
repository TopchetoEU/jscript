package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.List;

import com.github.bsideup.jabel.Desugar;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.values.VariableNode;

public class VariableDeclareNode extends Node {
	@Desugar
    public static record Pair(VariableNode var, Node value) { }

    public final List<Pair> values;

    @Override public void resolve(CompileResult target) {
		for (var entry : values) {
			target.scope.define(entry.var.name);
		}
    }
	@Override public void compileFunctions(CompileResult target) {
		for (var pair : values) {
			if (pair.value != null) pair.value.compileFunctions(target);
		}
	}
    @Override public void compile(CompileResult target, boolean pollute) {
        for (var entry : values) {
            if (entry.value != null) {
                entry.value.compile(target, true);
				target.add(VariableNode.toSet(target, loc(), entry.var.name, false, true));
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

        var declType = JavaScript.parseDeclarationType(src, i + n);
        if (!declType.isSuccess()) return declType.chainError();
        n += declType.n;

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

            Node val = null;
            var endN = n;
            n += Parsing.skipEmpty(src, i + n);

            if (src.is(i + n, "=")) {
                n++;

                var valRes = JavaScript.parseExpression(src, i + n, 2);
                if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after '='");

                n += valRes.n;
                endN = n;
                n += Parsing.skipEmpty(src, i + n);
                val = valRes.result;
            }

            res.add(new Pair(new VariableNode(nameLoc, name.result), val));

            if (src.is(i + n, ",")) {
                n++;
                continue;
            }

            end = JavaScript.parseStatementEnd(src, i + endN);

            if (end.isSuccess()) {
                n += end.n + endN - n;
                return ParseRes.res(new VariableDeclareNode(loc, res), n);
            }
            else return end.chainError(src.loc(i + n), "Expected a comma or end of statement");
        }
    }
}
