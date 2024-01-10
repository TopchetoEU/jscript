package me.topchetoeu.jscript.core.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.CompileTarget;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.compilation.Statement;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.core.engine.values.Values;

public class LazyOrStatement extends Statement {
    public final Statement first, second;

    @Override public boolean pure() { return first.pure() && second.pure(); }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        if (first instanceof ConstantStatement) {
            if (Values.not(((ConstantStatement)first).value)) {
                second.compile(target, scope, pollute);
            }
            else first.compile(target, scope, pollute);
            return;
        }

        first.compile(target, scope, true);
        if (pollute) target.add(Instruction.dup(loc()));
        int start = target.size();
        target.add(Instruction.nop(null));
        if (pollute) target.add(Instruction.discard(loc()));
        second.compile(target, scope, pollute);
        target.set(start, Instruction.jmpIf(loc(), target.size() - start));
    }

    public LazyOrStatement(Location loc, Statement first, Statement second) {
        super(loc);
        this.first = first;
        this.second = second;
    }
}
