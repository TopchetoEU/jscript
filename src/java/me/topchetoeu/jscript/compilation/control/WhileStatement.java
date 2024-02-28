package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.common.Instruction.Type;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;

public class WhileStatement extends Statement {
    public final Statement condition, body;
    public final String label;

    @Override
    public void declare(CompileResult target) {
        body.declare(target);
    }
    @Override
    public void compile(CompileResult target, boolean pollute) {
        int start = target.size();
        condition.compile(target, true);
        int mid = target.temp();
        body.compile(target, false, BreakpointType.STEP_OVER);

        int end = target.size();

        replaceBreaks(target, label, mid + 1, end, start, end + 1);

        target.add(Instruction.jmp(start - end));
        target.set(mid, Instruction.jmpIfNot(end - mid + 1));
        if (pollute) target.add(Instruction.pushUndefined());
    }

    public WhileStatement(Location loc, String label, Statement condition, Statement body) {
        super(loc);
        this.label = label;
        this.condition = condition;
        this.body = body;
    }

    public static void replaceBreaks(CompileResult target, String label, int start, int end, int continuePoint, int breakPoint) {
        for (int i = start; i < end; i++) {
            var instr = target.get(i);
            if (instr.type == Type.NOP && instr.is(0, "cont") && (instr.get(1) == null || instr.is(1, label))) {
                target.set(i, Instruction.jmp(continuePoint - i));
            }
            if (instr.type == Type.NOP && instr.is(0, "break") && (instr.get(1) == null || instr.is(1, label))) {
                target.set(i, Instruction.jmp(breakPoint - i));
            }
        }
    }
}
