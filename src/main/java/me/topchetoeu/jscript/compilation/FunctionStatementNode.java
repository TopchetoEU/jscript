package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.scope.Variable;
import me.topchetoeu.jscript.compilation.values.VariableNode;

public class FunctionStatementNode extends FunctionNode {
    public final String name;

    @Override public String name() { return name; }

    @Override public void resolve(CompileResult target) {
        target.scope.define(new Variable(name, false), end);
    }

    @Override public void compile(CompileResult target, boolean pollute, String name, BreakpointType bp) {
        var id = target.addChild(compileBody(target, true, name, null));
        target.add(_i -> Instruction.loadFunc(id, true, true, false, name, captures(id, target)));
        target.add(VariableNode.toSet(target, end, this.name, pollute, true));
    }

    public FunctionStatementNode(Location loc, Location end, Parameters params, CompoundNode body, String name) {
        super(loc, end, params, body);
        this.name = name;
    }
}
