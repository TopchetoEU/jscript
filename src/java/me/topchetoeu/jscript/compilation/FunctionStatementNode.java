package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.values.VariableNode;

public class FunctionStatementNode extends FunctionNode {
    public final String name;

    @Override public String name() { return name; }

    @Override public void resolve(CompileResult target) {
        target.scope.define(name, false, end);
    }

    @Override public void compile(CompileResult target, boolean pollute, String name, BreakpointType bp) {
        compile(target, true, false, this.name, bp);
        target.add(VariableNode.toSet(target, end, this.name, pollute, true));
    }

    public FunctionStatementNode(Location loc, Location end, String[] args, CompoundNode body, String name) {
        super(loc, end, args, body);
        this.name = name;
    }
}
