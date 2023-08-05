package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.engine.values.Values;

public class LazyAndStatement extends Statement {
    public final Statement first, second;

    @Override
    public boolean pollutesStack() { return true; }
    @Override
    public boolean pure() {
        return first.pure() && second.pure();
    }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        if (first instanceof ConstantStatement) {
            if (Values.not(((ConstantStatement)first).value)) {
                first.compileWithPollution(target, scope);
            }
            else second.compileWithPollution(target, scope);
            return;
        }

        first.compileWithPollution(target, scope);
        target.add(Instruction.dup(1).locate(loc()));
        int start = target.size();
        target.add(Instruction.nop());
        target.add(Instruction.discard().locate(loc()));
        second.compileWithPollution(target, scope);
        target.set(start, Instruction.jmpIfNot(target.size() - start).locate(loc()));
    }

    public LazyAndStatement(Location loc, Statement first, Statement second) {
        super(loc);
        this.first = first;
        this.second = second;
    }
}
