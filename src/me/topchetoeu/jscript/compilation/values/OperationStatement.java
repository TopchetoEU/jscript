package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class OperationStatement extends Statement {
    public final Statement[] args;
    public final Operation operation;

    @Override public boolean pure() {
        for (var el : args) {
            if (!el.pure()) return false;
        }

        return true;
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        for (var arg : args) {
            arg.compile(target, scope, true);
        }

        if (pollute) target.add(Instruction.operation(loc(), operation));
        else target.add(Instruction.discard(loc()));
    }

    public OperationStatement(Location loc, Operation operation, Statement ...args) {
        super(loc);
        this.operation = operation;
        this.args = args;
    }
}
