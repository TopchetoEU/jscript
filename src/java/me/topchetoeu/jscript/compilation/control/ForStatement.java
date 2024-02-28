package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;

public class ForStatement extends Statement {
    public final Statement declaration, assignment, condition, body;
    public final String label;

    @Override
    public void declare(CompileResult target) {
        declaration.declare(target);
        body.declare(target);
    }
    @Override
    public void compile(CompileResult target, boolean pollute) {
        declaration.compile(target, false, BreakpointType.STEP_OVER);

        int start = target.size();
        condition.compile(target, true, BreakpointType.STEP_OVER);
        int mid = target.temp();
        body.compile(target, false, BreakpointType.STEP_OVER);
        int beforeAssign = target.size();
        assignment.compile(target, false, BreakpointType.STEP_OVER);
        int end = target.size();

        WhileStatement.replaceBreaks(target, label, mid + 1, end, beforeAssign, end + 1);

        target.add(Instruction.jmp(start - end));
        target.set(mid, Instruction.jmpIfNot(end - mid + 1));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public ForStatement(Location loc, String label, Statement declaration, Statement condition, Statement assignment, Statement body) {
        super(loc);
        this.label = label;
        this.declaration = declaration;
        this.condition = condition;
        this.assignment = assignment;
        this.body = body;
    }
}
