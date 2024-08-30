package me.topchetoeu.jscript.compilation.values.operations;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.common.parsing.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.values.FunctionStatement;

public class VariableAssignStatement extends Statement {
    public final String name;
    public final Statement value;
    public final Operation operation;

    @Override public boolean pure() { return false; }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        var i = target.scope.getKey(name);
        if (operation != null) {
            target.add(Instruction.loadVar(i));
            FunctionStatement.compileWithName(value, target, true, name);
            target.add(Instruction.operation(operation));
            target.add(Instruction.storeVar(i, pollute));
        }
        else {
            FunctionStatement.compileWithName(value, target, true, name);
            target.add(Instruction.storeVar(i, pollute));
        }
    }

    public VariableAssignStatement(Location loc, String name, Statement val, Operation operation) {
        super(loc);
        this.name = name;
        this.value = val;
        this.operation = operation;
    }
}
