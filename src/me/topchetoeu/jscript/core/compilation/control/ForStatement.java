package me.topchetoeu.jscript.core.compilation.control;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.CompileTarget;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.compilation.Statement;
import me.topchetoeu.jscript.core.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;

public class ForStatement extends Statement {
    public final Statement declaration, assignment, condition, body;
    public final String label;

    @Override
    public void declare(ScopeRecord globScope) {
        declaration.declare(globScope);
        body.declare(globScope);
    }
    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        declaration.compile(target, scope, false, BreakpointType.STEP_OVER);

        int start = target.size();
        condition.compile(target, scope, true, BreakpointType.STEP_OVER);
        int mid = target.size();
        target.add(Instruction.nop(null));
        body.compile(target, scope, false, BreakpointType.STEP_OVER);
        int beforeAssign = target.size();
        assignment.compile(target, scope, false, BreakpointType.STEP_OVER);
        int end = target.size();

        WhileStatement.replaceBreaks(target, label, mid + 1, end, beforeAssign, end + 1);

        target.add(Instruction.jmp(loc(), start - end));
        target.set(mid, Instruction.jmpIfNot(loc(), end - mid + 1));
        if (pollute) target.add(Instruction.loadValue(loc(), null));
    }

    public ForStatement(Location loc, String label, Statement declaration, Statement condition, Statement assignment, Statement body) {
        super(loc);
        this.label = label;
        this.declaration = declaration;
        this.condition = condition;
        this.assignment = assignment;
        this.body = body;
    }
}
