package me.topchetoeu.jscript.core.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.AssignableStatement;
import me.topchetoeu.jscript.core.compilation.CompileTarget;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.compilation.Statement;
import me.topchetoeu.jscript.core.engine.Operation;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;

public class VariableStatement extends AssignableStatement {
    public final String name;

    @Override public boolean pure() { return false; }

    @Override
    public Statement toAssign(Statement val, Operation operation) {
        return new VariableAssignStatement(loc(), name, val, operation);
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        var i = scope.getKey(name);
        target.add(Instruction.loadVar(loc(), i));
        if (!pollute) target.add(Instruction.discard(loc()));
    }

    public VariableStatement(Location loc, String name) {
        super(loc);
        this.name = name;
    }
}
