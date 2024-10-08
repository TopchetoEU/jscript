package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.FunctionNode;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.values.VariableNode;

public class VariableAssignNode extends Node {
    public final String name;
    public final Node value;
    public final Operation operation;

    @Override public void compile(CompileResult target, boolean pollute) {
        if (operation != null) {
            target.add(VariableNode.toGet(target, loc(), name));
            FunctionNode.compileWithName(value, target, true, name);
            target.add(Instruction.operation(operation));
            target.add(VariableNode.toSet(target, loc(), name, pollute));
        }
        else {
            FunctionNode.compileWithName(value, target, true, name);
            target.add(VariableNode.toSet(target, loc(), name, pollute));
        }
    }

    public VariableAssignNode(Location loc, String name, Node val, Operation operation) {
        super(loc);
        this.name = name;
        this.value = val;
        this.operation = operation;
    }
}
