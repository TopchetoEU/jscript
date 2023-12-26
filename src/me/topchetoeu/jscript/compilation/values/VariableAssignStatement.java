package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class VariableAssignStatement extends Statement {
    public final String name;
    public final Statement value;
    public final Operation operation;

    @Override public boolean pure() { return false; }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        var i = scope.getKey(name);
        if (operation != null) {
            target.add(Instruction.loadVar(loc(), i));
            FunctionStatement.compileWithName(value, target, scope, true, name);
            target.add(Instruction.operation(loc(), operation));
            target.add(Instruction.storeVar(loc(), i, pollute));
        }
        else {
            FunctionStatement.compileWithName(value, target, scope, true, name);
            target.add(Instruction.storeVar(loc(), i, pollute));
        }
    }

    public VariableAssignStatement(Location loc, String name, Statement val, Operation operation) {
        super(loc);
        this.name = name;
        this.value = val;
        this.operation = operation;
    }
}
