package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.engine.values.Values;

public class LazyOrStatement extends Statement {
    public final Statement first, second;

    @Override
    public boolean pure() {
        return first.pure() && second.pure();
    }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope, boolean pollute) {
        if (first instanceof ConstantStatement) {
            if (Values.not(((ConstantStatement)first).value)) {
                second.compile(target, scope, pollute);
            }
            else first.compile(target, scope, pollute);
            return;
        }

        first.compile(target, scope, true);
        if (pollute) target.add(Instruction.dup().locate(loc()));
        int start = target.size();
        target.add(Instruction.nop());
        target.add(Instruction.discard().locate(loc()));
        second.compile(target, scope, pollute);
        target.set(start, Instruction.jmpIf(target.size() - start).locate(loc()));
    }

    public LazyOrStatement(Location loc, Statement first, Statement second) {
        super(loc);
        this.first = first;
        this.second = second;
    }
}
