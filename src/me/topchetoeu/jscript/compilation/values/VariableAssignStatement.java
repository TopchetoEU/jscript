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

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        var i = scope.getKey(name);
        if (operation != null) {
            target.add(Instruction.loadVar(i).locate(loc()));
            if (value instanceof FunctionStatement) ((FunctionStatement)value).compile(target, scope, name, false);
            else value.compile(target, scope, true);
            target.add(Instruction.operation(operation).locate(loc()));
            target.add(Instruction.storeVar(i, false).locate(loc()));
        }
        else {
            if (value instanceof FunctionStatement) ((FunctionStatement)value).compile(target, scope, name, false);
            else value.compile(target, scope, true);
            target.add(Instruction.storeVar(i, false).locate(loc()));
        }

        if (pollute) target.add(Instruction.loadValue(null).locate(loc()));
    }

    public VariableAssignStatement(Location loc, String name, Statement val, Operation operation) {
        super(loc);
        this.name = name;
        this.value = val;
        this.operation = operation;
    }
}
