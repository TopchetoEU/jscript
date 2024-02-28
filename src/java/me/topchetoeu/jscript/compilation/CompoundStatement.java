package me.topchetoeu.jscript.compilation;

import java.util.List;
import java.util.Vector;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.common.Instruction.BreakpointType;
import me.topchetoeu.jscript.compilation.values.FunctionStatement;

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
    public void declare(CompileResult target) {
        for (var stm : statements) stm.declare(target);
    }

    @Override
    public void compile(CompileResult target, boolean pollute, BreakpointType type) {
        List<Statement> statements = new Vector<Statement>();
        if (separateFuncs) for (var stm : this.statements) {
            if (stm instanceof FunctionStatement && ((FunctionStatement)stm).statement) {
                stm.compile(target, false);
            }
            else statements.add(stm);
        }
        else statements = List.of(this.statements);

        var polluted = false;

        for (var i = 0; i < statements.size(); i++) {
            var stm = statements.get(i);

            if (i != statements.size() - 1) stm.compile(target, false, BreakpointType.STEP_OVER);
            else stm.compile(target, polluted = pollute, BreakpointType.STEP_OVER);
        }

        if (!polluted && pollute) {
            target.add(Instruction.pushUndefined());
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
