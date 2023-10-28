package me.topchetoeu.jscript.compilation.control;

import java.util.HashMap;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
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
        var caseMap = new HashMap<Integer, Integer>();
        var stmIndexMap = new HashMap<Integer, Integer>();

        value.compile(target, scope, true);

        for (var ccase : cases) {
            target.add(Instruction.dup().locate(loc()));
            ccase.value.compile(target, scope, true);
            target.add(Instruction.operation(Operation.EQUALS).locate(loc()));
            caseMap.put(target.size(), ccase.statementI);
            target.add(Instruction.nop());
        }

        int start = target.size();

        target.add(Instruction.nop());

        for (var stm : body) {
            stmIndexMap.put(stmIndexMap.size(), target.size());
            stm.compileWithDebug(target, scope, false);
        }

        if (defaultI < 0 || defaultI >= body.length) target.set(start, Instruction.jmp(target.size() - start).locate(loc()));
        else target.set(start, Instruction.jmp(stmIndexMap.get(defaultI) - start)).locate(loc());

        for (int i = start; i < target.size(); i++) {
            var instr = target.get(i);
            if (instr.type == Type.NOP && instr.is(0, "break") && instr.get(1) == null) {
                target.set(i, Instruction.jmp(target.size() - i).locate(instr.location));
            }
        }
        for (var el : caseMap.entrySet()) {
            var loc = target.get(el.getKey()).location;
            var i = stmIndexMap.get(el.getValue());
            if (i == null) i = target.size();
            target.set(el.getKey(), Instruction.jmpIf(i - el.getKey()).locate(loc));
            target.setDebug(el.getKey());
        }

        target.add(Instruction.discard().locate(loc()));
    }

    public SwitchStatement(Location loc, Statement value, int defaultI, SwitchCase[] cases, Statement[] body) {
        super(loc);
        this.value = value;
        this.defaultI = defaultI;
        this.cases = cases;
        this.body = body;
    }
}
