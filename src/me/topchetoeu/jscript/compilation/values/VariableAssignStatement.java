package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.AssignStatement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class VariableAssignStatement extends AssignStatement {
    public final String name;
    public final Statement value;
    public final Operation operation;

    @Override
    public boolean pollutesStack() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope, boolean retPrevValue) {
        var i = scope.getKey(name);
        if (operation != null) {
            target.add(Instruction.loadVar(i).locate(loc()));
            if (retPrevValue) target.add(Instruction.dup().locate(loc()));
            if (value instanceof FunctionStatement) ((FunctionStatement)value).compile(target, scope, name, false);
            else value.compileWithPollution(target, scope);
            target.add(Instruction.operation(operation).locate(loc()));
            target.add(Instruction.storeVar(i, !retPrevValue).locate(loc()));
        }
        else {
            if (retPrevValue) target.add(Instruction.loadVar(i).locate(loc()));
            if (value instanceof FunctionStatement) ((FunctionStatement)value).compile(target, scope, name, false);
            else value.compileWithPollution(target, scope);
            target.add(Instruction.storeVar(i, !retPrevValue).locate(loc()));
        }
    }

    public VariableAssignStatement(Location loc, String name, Statement val, Operation operation) {
        super(loc);
        this.name = name;
        this.value = val;
        this.operation = operation;
    }
}
