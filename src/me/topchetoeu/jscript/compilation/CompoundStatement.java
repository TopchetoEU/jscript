package me.topchetoeu.jscript.compilation;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.values.FunctionStatement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class CompoundStatement extends Statement {
    public final Statement[] statements;
    public final boolean separateFuncs;
    public Location end;

    @Override public boolean pure() {
        for (var stm : statements) {
            if (!stm.pure()) return false;
        }

        return true;
    }

    @Override
    public void declare(ScopeRecord varsScope) {
        for (var stm : statements) stm.declare(varsScope);
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute, BreakpointType type) {
        if (separateFuncs) for (var stm : statements) {
            if (stm instanceof FunctionStatement && ((FunctionStatement)stm).statement) {
                stm.compile(target, scope, false);
            }
        }

        var polluted = false;

        for (var i = 0; i < statements.length; i++) {
            var stm = statements[i];

            if (separateFuncs && stm instanceof FunctionStatement) continue;
            if (i != statements.length - 1) stm.compile(target, scope, false, BreakpointType.STEP_OVER);
            else stm.compile(target, scope, polluted = pollute, BreakpointType.STEP_OVER);
        }

        if (!polluted && pollute) {
            target.add(Instruction.loadValue(loc(), null));
        }
    }

    public CompoundStatement setEnd(Location loc) {
        this.end = loc;
        return this;
    }

    public CompoundStatement(Location loc, boolean separateFuncs, Statement ...statements) {
        super(loc);
        this.separateFuncs = separateFuncs;
        this.statements = statements;
    }
}
