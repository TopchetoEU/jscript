package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;

public class IfStatement extends Statement {
    public final Statement condition, body, elseBody;

    @Override
    public void declare(CompileResult target) {
        body.declare(target);
        if (elseBody != null) elseBody.declare(target);
    }

    @Override public void compile(CompileResult target, boolean pollute, BreakpointType breakpoint) {
        condition.compile(target, true, breakpoint);

        if (elseBody == null) {
            int i = target.temp();
            body.compile(target, pollute, breakpoint);
            int endI = target.size();
            target.set(i, Instruction.jmpIfNot(endI - i));
        }
        else {
            int start = target.temp();
            body.compile(target, pollute, breakpoint);
            int mid = target.temp();
            elseBody.compile(target, pollute, breakpoint);
            int end = target.size();

            target.set(start, Instruction.jmpIfNot(mid - start - 1));
            target.set(mid, Instruction.jmp(end - mid));
        }
    }
    @Override public void compile(CompileResult target, boolean pollute) {
        compile(target, pollute, BreakpointType.STEP_IN);
    }

    public IfStatement(Location loc, Statement condition, Statement body, Statement elseBody) {
        super(loc);
        this.condition = condition;
        this.body = body;
        this.elseBody = elseBody;
    }
}
