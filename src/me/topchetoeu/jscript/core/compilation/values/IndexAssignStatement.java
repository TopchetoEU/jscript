package me.topchetoeu.jscript.core.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.CompileTarget;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.compilation.Statement;
import me.topchetoeu.jscript.core.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.core.engine.Operation;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;

public class IndexAssignStatement extends Statement {
    public final Statement object;
    public final Statement index;
    public final Statement value;
    public final Operation operation;

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        if (operation != null) {
            object.compile(target, scope, true);
            index.compile(target, scope, true);
            target.add(Instruction.dup(loc(), 2));

            target.add(Instruction.loadMember(loc()));
            value.compile(target, scope, true);
            target.add(Instruction.operation(loc(), operation));

            target.add(Instruction.storeMember(loc(), pollute));
            target.setDebug(BreakpointType.STEP_IN);
        }
        else {
            object.compile(target, scope, true);
            index.compile(target, scope, true);
            value.compile(target, scope, true);

            target.add(Instruction.storeMember(loc(), pollute));
            target.setDebug(BreakpointType.STEP_IN);
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
