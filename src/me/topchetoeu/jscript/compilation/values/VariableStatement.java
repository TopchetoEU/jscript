package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class VariableStatement extends AssignableStatement {
    public final String name;

    @Override
    public boolean pure() { return true; }

    @Override
    public Statement toAssign(Statement val, Operation operation) {
        return new VariableAssignStatement(loc(), name, val, operation);
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        var i = scope.getKey(name);
        target.add(Instruction.loadVar(i).locate(loc()));
        if (!pollute) target.add(Instruction.discard().locate(loc()));
    }

    public VariableStatement(Location loc, String name) {
        super(loc);
        this.name = name;
    }
}
