package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.common.parsing.ParseRes;
import me.topchetoeu.jscript.common.parsing.Parsing;
import me.topchetoeu.jscript.common.parsing.Source;
import me.topchetoeu.jscript.compilation.AssignableNode;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.JavaScript;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.values.operations.VariableAssignNode;

public class VariableNode extends Node implements AssignableNode {
    public final String name;

    @Override public boolean pure() { return false; }

    @Override
    public Node toAssign(Node val, Operation operation) {
        return new VariableAssignNode(loc(), name, val, operation);
    }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        var i = target.scope.getKey(name);
        target.add(Instruction.loadVar(i));
        if (!pollute) target.add(Instruction.discard());
    }

    public VariableNode(Location loc, String name) {
        super(loc);
        this.name = name;
    }

    public static ParseRes<VariableNode> parse(Source src, int i) {
        var n = Parsing.skipEmpty(src, i);
        var loc = src.loc(i + n);

        var literal = Parsing.parseIdentifier(src, i);
        if (!literal.isSuccess()) return literal.chainError();
        n += literal.n;

        if (!JavaScript.checkVarName(literal.result)) {
            if (literal.result.equals("await")) return ParseRes.error(src.loc(i + n), "'await' expressions are not supported.");
            if (literal.result.equals("const")) return ParseRes.error(src.loc(i + n), "'const' declarations are not supported.");
            if (literal.result.equals("let")) return ParseRes.error(src.loc(i + n), "'let' declarations are not supported.");
            return ParseRes.error(src.loc(i + n), String.format("Unexpected keyword '%s'.", literal.result));
        }

        return ParseRes.res(new VariableNode(loc, literal.result), n);
    }
}