package me.topchetoeu.jscript.compilation.control;

import java.util.HashMap;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

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
    public void declare(ScopeRecord varsScope) {
        for (var stm : body) stm.declare(varsScope);
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        var caseToStatement = new HashMap<Integer, Integer>();
        var statementToIndex = new HashMap<Integer, Integer>();

        value.compileWithDebug(target, scope, true, BreakpointType.STEP_OVER);

        for (var ccase : cases) {
            target.add(Instruction.dup(loc()));
            ccase.value.compile(target, scope, true);
            target.add(Instruction.operation(loc(), Operation.EQUALS));
            caseToStatement.put(target.size(), ccase.statementI);
            target.add(Instruction.nop(null));
        }

        int start = target.size();

        target.add(Instruction.nop(null));

        for (var stm : body) {
            statementToIndex.put(statementToIndex.size(), target.size());
            stm.compileWithDebug(target, scope, false, BreakpointType.STEP_OVER);
        }

        int end = target.size();
        target.add(Instruction.discard(loc()));
        if (pollute) target.add(Instruction.loadValue(loc(), null));

        if (defaultI < 0 || defaultI >= body.length) target.set(start, Instruction.jmp(loc(), end - start));
        else target.set(start, Instruction.jmp(loc(), statementToIndex.get(defaultI) - start));

        for (int i = start; i < end; i++) {
            var instr = target.get(i);
            if (instr.type == Type.NOP && instr.is(0, "break") && instr.get(1) == null) {
                target.set(i, Instruction.jmp(loc(), end - i).locate(instr.location));
            }
        }
        for (var el : caseToStatement.entrySet()) {
            var i = statementToIndex.get(el.getValue());
            if (i == null) i = end;
            target.set(el.getKey(), Instruction.jmpIf(loc(), i - el.getKey()));
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
