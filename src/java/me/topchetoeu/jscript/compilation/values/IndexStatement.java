package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.core.Operation;

public class IndexStatement extends AssignableStatement {
    public final Statement object;
    public final Statement index;

    @Override
    public Statement toAssign(Statement val, Operation operation) {
        return new IndexAssignStatement(loc(), object, index, val, operation);
    }
    public void compile(CompileResult target, boolean dupObj, boolean pollute) {
        object.compile(target, true);
        if (dupObj) target.add(Instruction.dup());

        index.compile(target, true);
        target.add(Instruction.loadMember()).setLocationAndDebug(loc(), BreakpointType.STEP_IN);
        if (!pollute) target.add(Instruction.discard());
    }
    @Override
    public void compile(CompileResult target, boolean pollute) {
        compile(target, false, pollute);
    }

    public IndexStatement(Location loc, Statement object, Statement index) {
        super(loc);
        this.object = object;
        this.index = index;
    }
}
