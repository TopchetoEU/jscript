package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.engine.values.Values;

public class TernaryStatement extends Statement {
    public final Statement condition;
    public final Statement first;
    public final Statement second;

    @Override
    public boolean pollutesStack() { return true; }
    @Override
    public boolean pure() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        if (condition instanceof ConstantStatement) {
            if (!Values.toBoolean(((ConstantStatement)condition).value)) {
                second.compileWithPollution(target, scope);
            }
            else first.compileWithPollution(target, scope);
            return;
        }

        condition.compileWithPollution(target, scope);
        int start = target.size();
        target.add(Instruction.nop());
        first.compileWithPollution(target, scope);
        int mid = target.size();
        target.add(Instruction.nop());
        second.compileWithPollution(target, scope);
        int end = target.size();

        target.set(start, Instruction.jmpIfNot(mid - start + 1).locate(loc()));
        target.set(mid, Instruction.jmp(end - mid).locate(loc()));
    }

    @Override
    public Statement optimize() {
        var cond = condition.optimize();
        var f = first.optimize();
        var s = second.optimize();
        return new TernaryStatement(loc(), cond, f, s);
    }

    public TernaryStatement(Location loc, Statement condition, Statement first, Statement second) {
        super(loc);
        this.condition = condition;
        this.first = first;
        this.second = second;
    }
}
