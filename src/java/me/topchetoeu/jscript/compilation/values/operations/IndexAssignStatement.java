package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;

public class IndexAssignStatement extends Statement {
    public final Statement object;
    public final Statement index;
    public final Statement value;
    public final Operation operation;

    @Override
    public void compile(CompileResult target, boolean pollute) {
        if (operation != null) {
            object.compile(target, true);
            index.compile(target, true);
            target.add(Instruction.dup(2));

            target.add(Instruction.loadMember());
            value.compile(target, true);
            target.add(Instruction.operation(operation));

            target.add(Instruction.storeMember(pollute)).setLocationAndDebug(loc(), BreakpointType.STEP_IN);
        }
        else {
            object.compile(target, true);
            index.compile(target, true);
            value.compile(target, true);

            target.add(Instruction.storeMember(pollute)).setLocationAndDebug(loc(), BreakpointType.STEP_IN);;
        }
    }

    public IndexAssignStatement(Location loc, Statement object, Statement index, Statement value, Operation operation) {
        super(loc);
        this.object = object;
        this.index = index;
        this.value = value;
        this.operation = operation;
    }
}
