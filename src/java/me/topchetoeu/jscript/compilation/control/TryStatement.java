package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;

public class TryStatement extends Statement {
    public final Statement tryBody;
    public final Statement catchBody;
    public final Statement finallyBody;
    public final String name;

    @Override
    public void declare(CompileResult target) {
        tryBody.declare(target);
        if (catchBody != null) catchBody.declare(target);
        if (finallyBody != null) finallyBody.declare(target);
    }

    @Override
    public void compile(CompileResult target, boolean pollute, BreakpointType bpt) {
        int replace = target.temp();

        int start = replace + 1, catchStart = -1, finallyStart = -1;

        tryBody.compile(target, false);
        target.add(Instruction.tryEnd());

        if (catchBody != null) {
            catchStart = target.size() - start;
            target.scope.define(name, true);
            catchBody.compile(target, false);
            target.scope.undefine();
            target.add(Instruction.tryEnd());
        }

        if (finallyBody != null) {
            finallyStart = target.size() - start;
            finallyBody.compile(target, false);
            target.add(Instruction.tryEnd());
        }

        target.set(replace, Instruction.tryStart(catchStart, finallyStart, target.size() - start));
        target.setLocationAndDebug(replace, loc(), BreakpointType.STEP_OVER);

        if (pollute) target.add(Instruction.pushUndefined());
    }

    public TryStatement(Location loc, Statement tryBody, Statement catchBody, Statement finallyBody, String name) {
        super(loc);
        this.tryBody = tryBody;
        this.catchBody = catchBody;
        this.finallyBody = finallyBody;
        this.name = name;
    }
}
