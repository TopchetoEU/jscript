package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.values.VariableNode;

public class TypeofNode extends Node {
    public final Node value;

    // Not really pure, since a variable from the global scope could be accessed,
    // which could lead to code execution, that would get omitted
    @Override public boolean pure() { return true; }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        if (value instanceof VariableNode) {
            var i = target.scope.getKey(((VariableNode)value).name);
            if (i instanceof String) {
                target.add(Instruction.typeof((String)i));
                return;
            }
        }
        value.compile(target, pollute);
        target.add(Instruction.typeof());
    }

    public TypeofNode(Location loc, Node value) {
        super(loc);
        this.value = value;
    }

    public static ParseRes<TypeofNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        if (!Parsing.isIdentifier(src, i + n, "typeof")) return ParseRes.failed();
        n += 6;

        var valRes = JavaScript.parseExpression(src, i + n, 15);
        if (!valRes.isSuccess()) return valRes.chainError(src.loc(i + n), "Expected a value after 'typeof' keyword.");
        n += valRes.n;
    
        return ParseRes.res(new TypeofNode(loc, valRes.result), n);
    }
}
