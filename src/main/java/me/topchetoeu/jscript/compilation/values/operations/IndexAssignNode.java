package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Node;
import me.topchetoeu.jscript.compilation.values.constants.NumberNode;
import me.topchetoeu.jscript.compilation.values.constants.StringNode;

public class IndexAssignNode extends Node {
    public final Node object;
    public final Node index;
    public final Node value;
    public final Operation operation;

    @Override public void compile(CompileResult target, boolean pollute) {
        if (operation != null) {
            object.compile(target, true);

            if (index instanceof NumberNode num && (int)num.value == num.value) {
                target.add(Instruction.loadMember((int)num.value));
                value.compile(target, true);
                target.add(Instruction.operation(operation));
                target.add(Instruction.storeMember((int)num.value, pollute));
            }
            else if (index instanceof StringNode str) {
                target.add(Instruction.loadMember(str.value));
                value.compile(target, true);
                target.add(Instruction.operation(operation));
                target.add(Instruction.storeMember(str.value, pollute));
            }
            else {
                index.compile(target, true);
                target.add(Instruction.dup(2));

                target.add(Instruction.loadMember());
                value.compile(target, true);
                target.add(Instruction.operation(operation));

                target.add(Instruction.storeMember(pollute));
            }
            target.setLocationAndDebug(loc(), BreakpointType.STEP_IN);
        }
        else {
            object.compile(target, true);

            if (index instanceof NumberNode num && (int)num.value == num.value) {
                value.compile(target, true);
                target.add(Instruction.storeMember((int)num.value, pollute));
            }
            else if (index instanceof StringNode str) {
                value.compile(target, true);
                target.add(Instruction.storeMember(str.value, pollute));
            }
            else {
                index.compile(target, true);
                value.compile(target, true);
                target.add(Instruction.storeMember(pollute));
            }

            target.setLocationAndDebug(loc(), BreakpointType.STEP_IN);;
        }
    }

    public IndexAssignNode(Location loc, Node object, Node index, Node value, Operation operation) {
        super(loc);
        this.object = object;
        this.index = index;
        this.value = value;
        this.operation = operation;
    }
}
