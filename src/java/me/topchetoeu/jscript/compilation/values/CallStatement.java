package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;

public class CallStatement extends Statement {
    public final Statement func;
    public final Statement[] args;
    public final boolean isNew;

    @Override
    public void compile(CompileResult target, boolean pollute, BreakpointType type) {
        if (isNew) func.compile(target, true);
        else if (func instanceof IndexStatement) {
            ((IndexStatement)func).compile(target, true, true);
        }
        else {
            target.add(Instruction.pushUndefined());
            func.compile(target, true);
        }

        for (var arg : args) arg.compile(target, true);

        if (isNew) target.add(Instruction.callNew(args.length));
        else target.add(Instruction.call(args.length)).setDebug(loc(), type);

        if (!pollute) target.add(Instruction.discard());
    }
    @Override
    public void compile(CompileResult target, boolean pollute) {
        compile(target, pollute, BreakpointType.STEP_IN);
    }

    public CallStatement(Location loc, boolean isNew, Statement func, Statement ...args) {
        super(loc);
        this.isNew = isNew;
        this.func = func;
        this.args = args;
    }
}
