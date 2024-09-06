package me.topchetoeu.jscript.compilation.values;

import java.util.ArrayList;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;


public class ArrayNode extends Node {
    public final Node[] statements;

    @Override public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.loadArr(statements.length));

        for (var i = 0; i < statements.length; i++) {
            var el = statements[i];
            if (el != null) {
                target.add(Instruction.dup());
                el.compile(target, true);
                target.add(Instruction.storeMember(i));
            }
        }

        if (!pollute) target.add(Instruction.discard());
    }

    public ArrayNode(Location loc, Node[] statements) {
        super(loc);
        this.statements = statements;
    }

    public static ParseRes<ArrayNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!src.is(i + n, "[")) return ParseRes.failed();
        n++;

        var values = new ArrayList<Node>();

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

            var res = JavaScript.parseExpression(src, i + n, 2);
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

        return ParseRes.res(new ArrayNode(loc, values.toArray(Node[]::new)), n);
    }
}
