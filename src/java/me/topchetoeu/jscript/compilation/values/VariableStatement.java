package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.core.Operation;

public class VariableStatement extends AssignableStatement {
    public final String name;

    @Override public boolean pure() { return false; }

    @Override
    public Statement toAssign(Statement val, Operation operation) {
        return new VariableAssignStatement(loc(), name, val, operation);
    }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        var i = target.scope.getKey(name);
        target.add(Instruction.loadVar(i));
        if (!pollute) target.add(Instruction.discard());
    }

    public VariableStatement(Location loc, String name) {
        super(loc);
        this.name = name;
    }
}
