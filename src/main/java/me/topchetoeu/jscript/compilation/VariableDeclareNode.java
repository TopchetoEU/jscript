package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.JavaScript.DeclarationType;
import me.topchetoeu.jscript.compilation.patterns.Pattern;

public class VariableDeclareNode extends Node {
    public static class Pair {
        public final Pattern destructor;
        public final Node value;
        public final Location location;

        public Pair(Pattern destr, Node value, Location location) {
            this.destructor = destr;
            this.value = value;
            this.location = location;
        }
    }

    public final List<Pair> values;
    public final DeclarationType declType;

    @Override public void resolve(CompileResult target) {
        if (!declType.strict) {
            for (var entry : values) {
                entry.destructor.destructDeclResolve(target);
            }
        }
    }
    @Override public void compile(CompileResult target, boolean pollute) {
        for (var entry : values) {
            if (entry.value == null) {
                if (declType == DeclarationType.VAR) entry.destructor.declare(target, null, false);
                else entry.destructor.declare(target, declType, false);
            }
            else {
                entry.value.compile(target, true);

                if (declType == DeclarationType.VAR) entry.destructor.destruct(target, null, true);
                else entry.destructor.destruct(target, declType, true);
            }
        }

        if (pollute) target.add(Instruction.pushUndefined());
    }

    public VariableDeclareNode(Location loc, DeclarationType declType, List<Pair> values) {
        super(loc);
        this.values = values;
        this.declType = declType;
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
            return ParseRes.res(new VariableDeclareNode(loc, declType.result, res), n);
        }

        while (true) {
            var nameLoc = src.loc(i + n);

            var name = Pattern.parse(src, i + n, false);
            if (!name.isSuccess()) return name.chainError(nameLoc, "Expected a variable name or a destructor");
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

            res.add(new Pair(name.result, val, nameLoc));

            if (src.is(i + n, ",")) {
                n++;
                continue;
            }

            end = JavaScript.parseStatementEnd(src, i + endN);

            if (end.isSuccess()) {
                n += end.n + endN - n;
                return ParseRes.res(new VariableDeclareNode(loc, declType.result, res), n);
            }
            else return end.chainError(src.loc(i + n), "Expected a comma or end of statement");
        }
    }
}
