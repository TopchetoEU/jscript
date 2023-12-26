package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class WhileStatement extends Statement {
    public final Statement condition, body;
    public final String label;

    @Override
    public void declare(ScopeRecord globScope) {
        body.declare(globScope);
    }
    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        int start = target.size();
        condition.compile(target, scope, true);
        int mid = target.size();
        target.add(Instruction.nop(null));
        body.compile(target, scope, false, BreakpointType.STEP_OVER);

        int end = target.size();

        replaceBreaks(target, label, mid + 1, end, start, end + 1);

        target.add(Instruction.jmp(loc(), start - end));
        target.set(mid, Instruction.jmpIfNot(loc(), end - mid + 1));
        if (pollute) target.add(Instruction.loadValue(loc(), null));
    }

    public WhileStatement(Location loc, String label, Statement condition, Statement body) {
        super(loc);
        this.label = label;
        this.condition = condition;
        this.body = body;
    }

    public static void replaceBreaks(CompileTarget target, String label, int start, int end, int continuePoint, int breakPoint) {
        for (int i = start; i < end; i++) {
            var instr = target.get(i);
            if (instr.type == Type.NOP && instr.is(0, "cont") && (instr.get(1) == null || instr.is(1, label))) {
                target.set(i, Instruction.jmp(instr.location, continuePoint - i).setDbgData(target.get(i)));
            }
            if (instr.type == Type.NOP && instr.is(0, "break") && (instr.get(1) == null || instr.is(1, label))) {
                target.set(i, Instruction.jmp(instr.location, breakPoint - i).setDbgData(target.get(i)));
            }
        }
    }
}
