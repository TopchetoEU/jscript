package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class VariableAssignStatement extends Statement {
    public final String name;
    public final Statement value;
    public final Type operation;

    @Override
    public boolean pollutesStack() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        var i = scope.getKey(name);
        if (operation != null) {
            target.add(Instruction.loadVar(i).locate(loc()));
            if (value instanceof FunctionStatement) ((FunctionStatement)value).compile(target, scope, name, false);
            else value.compileWithPollution(target, scope);
            target.add(Instruction.operation(operation).locate(loc()));
        }
        else {
            if (value instanceof FunctionStatement) ((FunctionStatement)value).compile(target, scope, name, false);
            else value.compileWithPollution(target, scope);
        }

        target.add(Instruction.storeVar(i, true).locate(loc()));
    }

    public VariableAssignStatement(Location loc, String name, Statement val, Type operation) {
        super(loc);
        this.name = name;
        this.value = val;
        this.operation = operation;
    }
}
