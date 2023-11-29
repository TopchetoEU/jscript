package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.engine.values.Values;

public class LazyAndStatement extends Statement {
    public final Statement first, second;

    @Override
    public boolean pure() {
        return first.pure() && second.pure();
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        if (first instanceof ConstantStatement) {
            if (Values.not(((ConstantStatement)first).value)) {
                first.compile(target, scope, pollute);
            }
            else second.compile(target, scope, pollute);
            return;
        }

        first.compile(target, scope, true);
        if (pollute) target.add(Instruction.dup(loc()));
        int start = target.size();
        target.add(Instruction.nop(null));
        if (pollute) target.add(Instruction.discard(loc()));
        second.compile(target, scope, pollute);
        target.set(start, Instruction.jmpIfNot(loc(), target.size() - start));
    }

    public LazyAndStatement(Location loc, Statement first, Statement second) {
        super(loc);
        this.first = first;
        this.second = second;
    }
}
