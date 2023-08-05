package me.topchetoeu.jscript.compilation.control;

import java.util.HashMap;
import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class SwitchStatement extends Statement {
    public static record SwitchCase(Statement value, int statementI) {}

    @Override
    public boolean pollutesStack() { return false; }

    public final Statement value;
    public final SwitchCase[] cases;
    public final Statement[] body;
    public final int defaultI;

    @Override
    public void declare(ScopeRecord varsScope) {
        for (var stm : body) stm.declare(varsScope);
    }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        var caseMap = new HashMap<Integer, Integer>();
        var stmIndexMap = new HashMap<Integer, Integer>();

        value.compile(target, scope);

        for (var ccase : cases) {
            target.add(Instruction.dup(1).locate(loc()));
            ccase.value.compileWithPollution(target, scope);
            target.add(Instruction.operation(Type.EQUALS).locate(loc()));
            caseMap.put(target.size(), ccase.statementI);
            target.add(Instruction.nop());
        }

        int start = target.size();

        target.add(Instruction.nop());

        for (var stm : body) {
            stmIndexMap.put(stmIndexMap.size(), target.size());
            stm.compileNoPollution(target, scope, true);
        }

        if (defaultI < 0 || defaultI >= body.length) target.set(start, Instruction.jmp(target.size() - start).locate(loc()));
        else target.set(start, Instruction.jmp(stmIndexMap.get(defaultI) - start)).locate(loc());

        for (int i = start; i < target.size(); i++) {
            var instr = target.get(i);
            if (instr.type == Type.NOP && instr.is(0, "break") && instr.get(1) == null) {
                target.set(i, Instruction.jmp(target.size() - i).locate(instr.location));
            }
            if (instr.type == Type.NOP && instr.is(0, "try_break") && instr.get(1) == null) {
                target.set(i, Instruction.signal("jmp_" + (target.size() - (Integer)instr.get(2))).locate(instr.location));
            }
        }
        for (var el : caseMap.entrySet()) {
            var loc = target.get(el.getKey()).location;
            var i = stmIndexMap.get(el.getValue());
            if (i == null) i = target.size();
            target.set(el.getKey(), Instruction.jmpIf(i - el.getKey()).locate(loc).setDebug(true));
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
