package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Operation;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;

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
    public void compile(CompileResult target, boolean pollute) {
        for (var arg : args) {
            arg.compile(target, true);
        }

        if (pollute) target.add(Instruction.operation(operation));
        else target.add(Instruction.discard());
    }

    public OperationStatement(Location loc, Operation operation, Statement ...args) {
        super(loc);
        this.operation = operation;
        this.args = args;
    }
}
