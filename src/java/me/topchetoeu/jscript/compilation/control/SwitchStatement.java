package me.topchetoeu.jscript.compilation.control;

import java.util.HashMap;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.core.Operation;

public class SwitchStatement extends Statement {
    public static class SwitchCase {
        public final Statement value;
        public final int statementI;

        public SwitchCase(Statement value, int statementI) {
            this.value = value;
            this.statementI = statementI;
        }
    }

    public final Statement value;
    public final SwitchCase[] cases;
    public final Statement[] body;
    public final int defaultI;

    @Override
    public void declare(CompileResult target) {
        for (var stm : body) stm.declare(target);
    }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        var caseToStatement = new HashMap<Integer, Integer>();
        var statementToIndex = new HashMap<Integer, Integer>();

        value.compile(target, true, BreakpointType.STEP_OVER);

        for (var ccase : cases) {
            target.add(Instruction.dup());
            ccase.value.compile(target, true);
            target.add(Instruction.operation(Operation.EQUALS));
            caseToStatement.put(target.temp(), ccase.statementI);
        }

        int start = target.temp();

        for (var stm : body) {
            statementToIndex.put(statementToIndex.size(), target.size());
            stm.compile(target, false, BreakpointType.STEP_OVER);
        }

        int end = target.size();
        target.add(Instruction.discard());
        if (pollute) target.add(Instruction.pushUndefined());

        if (defaultI < 0 || defaultI >= body.length) target.set(start, Instruction.jmp(end - start));
        else target.set(start, Instruction.jmp(statementToIndex.get(defaultI) - start));

        for (int i = start; i < end; i++) {
            var instr = target.get(i);
            if (instr.type == Type.NOP && instr.is(0, "break") && instr.get(1) == null) {
                target.set(i, Instruction.jmp(end - i));
            }
        }
        for (var el : caseToStatement.entrySet()) {
            var i = statementToIndex.get(el.getValue());
            if (i == null) i = end;
            target.set(el.getKey(), Instruction.jmpIf(i - el.getKey()));
        }

    }

    public SwitchStatement(Location loc, Statement value, int defaultI, SwitchCase[] cases, Statement[] body) {
        super(loc);
        this.value = value;
        this.defaultI = defaultI;
        this.cases = cases;
        this.body = body;
    }
}
