package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class DoWhileStatement extends Statement {
    public final Statement condition, body;
    public final String label;

    @Override
    public void declare(ScopeRecord globScope) {
        body.declare(globScope);
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        int start = target.size();
        body.compileWithDebug(target, scope, false, BreakpointType.STEP_OVER);
        int mid = target.size();
        condition.compileWithDebug(target, scope, true, BreakpointType.STEP_OVER);
        int end = target.size();

        WhileStatement.replaceBreaks(target, label, start, mid - 1, mid, end + 1);
        target.add(Instruction.jmpIf(loc(), start - end));
    }

    public DoWhileStatement(Location loc, String label, Statement condition, Statement body) {
        super(loc);
        this.label = label;
        this.condition = condition;
        this.body = body;
    }
}
