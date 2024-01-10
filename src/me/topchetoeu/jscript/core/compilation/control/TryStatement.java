package me.topchetoeu.jscript.core.compilation.control;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.core.compilation.CompileTarget;
import me.topchetoeu.jscript.core.compilation.Instruction;
import me.topchetoeu.jscript.core.compilation.Statement;
import me.topchetoeu.jscript.core.compilation.Instruction.BreakpointType;
import me.topchetoeu.jscript.core.engine.scope.GlobalScope;
import me.topchetoeu.jscript.core.engine.scope.LocalScopeRecord;
import me.topchetoeu.jscript.core.engine.scope.ScopeRecord;

public class TryStatement extends Statement {
    public final Statement tryBody;
    public final Statement catchBody;
    public final Statement finallyBody;
    public final String name;

    @Override
    public void declare(ScopeRecord globScope) {
        tryBody.declare(globScope);
        if (catchBody != null) catchBody.declare(globScope);
        if (finallyBody != null) finallyBody.declare(globScope);
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute, BreakpointType bpt) {
        target.add(Instruction.nop(null));

        int start = target.size(), catchStart = -1, finallyStart = -1;

        tryBody.compile(target, scope, false);
        target.add(Instruction.tryEnd(loc()));

        if (catchBody != null) {
            catchStart = target.size() - start;
            var local = scope instanceof GlobalScope ? scope.child() : (LocalScopeRecord)scope;
            local.define(name, true);
            catchBody.compile(target, scope, false);
            local.undefine();
            target.add(Instruction.tryEnd(loc()));
        }

        if (finallyBody != null) {
            finallyStart = target.size() - start;
            finallyBody.compile(target, scope, false);
            target.add(Instruction.tryEnd(loc()));
        }

        target.set(start - 1, Instruction.tryStart(loc(), catchStart, finallyStart, target.size() - start));
        target.setDebug(start - 1, BreakpointType.STEP_OVER);
        if (pollute) target.add(Instruction.loadValue(loc(), null));
    }

    public TryStatement(Location loc, Statement tryBody, Statement catchBody, Statement finallyBody, String name) {
        super(loc);
        this.tryBody = tryBody;
        this.catchBody = catchBody;
        this.finallyBody = finallyBody;
        this.name = name;
    }
}
