package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;

public class DoWhileStatement extends Statement {
    public final Statement condition, body;
    public final String label;

    @Override
    public void declare(CompileResult target) {
        body.declare(target);
    }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        int start = target.size();
        body.compile(target, false, BreakpointType.STEP_OVER);
        int mid = target.size();
        condition.compile(target, true, BreakpointType.STEP_OVER);
        int end = target.size();

        WhileStatement.replaceBreaks(target, label, start, mid - 1, mid, end + 1);
        target.add(Instruction.jmpIf(start - end));
    }

    public DoWhileStatement(Location loc, String label, Statement condition, Statement body) {
        super(loc);
        this.label = label;
        this.condition = condition;
        this.body = body;
    }
}
