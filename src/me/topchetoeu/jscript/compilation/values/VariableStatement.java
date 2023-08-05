package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class VariableStatement extends AssignableStatement {
    public final String name;

    @Override
    public boolean pollutesStack() { return true; }
    @Override
    public boolean pure() { return true; }

    @Override
    public Statement toAssign(Statement val, Type operation) {
        return new VariableAssignStatement(loc(), name, val, operation);
    }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        var i = scope.getKey(name);
        target.add(Instruction.loadVar(i).locate(loc()));
    }

    public VariableStatement(Location loc, String name) {
        super(loc);
        this.name = name;
    }
}
