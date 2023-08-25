package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.AssignStatement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class IndexAssignStatement extends AssignStatement {
    public final Statement object;
    public final Statement index;
    public final Statement value;
    public final Operation operation;

    @Override
    public boolean pollutesStack() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope, boolean retPrevValue) {
        int start = 0;

        if (operation != null) {
            object.compileWithPollution(target, scope);
            index.compileWithPollution(target, scope);
            target.add(Instruction.dup(2, 0).locate(loc()));

            target.add(Instruction.loadMember().locate(loc()));
            if (retPrevValue) {
                target.add(Instruction.dup().locate(loc()));
                target.add(Instruction.move(3, 1).locate(loc()));
            }
            value.compileWithPollution(target, scope);
            target.add(Instruction.operation(operation).locate(loc()));

            target.add(Instruction.storeMember(!retPrevValue).locate(loc()).setDebug(true));
        }
        else {
            object.compileWithPollution(target, scope);
            if (retPrevValue) target.add(Instruction.dup().locate(loc()));
            index.compileWithPollution(target, scope);
            value.compileWithPollution(target, scope);

            target.add(Instruction.storeMember(!retPrevValue).locate(loc()).setDebug(true));
        }
        target.get(start);
    }

    public IndexAssignStatement(Location loc, Statement object, Statement index, Statement value, Operation operation) {
        super(loc);
        this.object = object;
        this.index = index;
        this.value = value;
        this.operation = operation;
    }
}
