package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class IfStatement extends Statement {
    public final Statement condition, body, elseBody;

    @Override
    public void declare(ScopeRecord globScope) {
        body.declare(globScope);
        if (elseBody != null) elseBody.declare(globScope);
    }

    @Override public void compile(CompileTarget target, ScopeRecord scope, boolean pollute, BreakpointType breakpoint) {
        condition.compile(target, scope, true, breakpoint);

        if (elseBody == null) {
            int i = target.size();
            target.add(Instruction.nop(null));
            body.compile(target, scope, pollute, breakpoint);
            int endI = target.size();
            target.set(i, Instruction.jmpIfNot(loc(), endI - i));
        }
        else {
            int start = target.size();
            target.add(Instruction.nop(null));
            body.compile(target, scope, pollute, breakpoint);
            target.add(Instruction.nop(null));
            int mid = target.size();
            elseBody.compile(target, scope, pollute, breakpoint);
            int end = target.size();

            target.set(start, Instruction.jmpIfNot(loc(), mid - start));
            target.set(mid - 1, Instruction.jmp(loc(), end - mid + 1));
        }
    }
    @Override public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        compile(target, scope, pollute, BreakpointType.STEP_IN);
    }

    public IfStatement(Location loc, Statement condition, Statement body, Statement elseBody) {
        super(loc);
        this.condition = condition;
        this.body = body;
        this.elseBody = elseBody;
    }
}
