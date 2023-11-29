package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.GlobalScope;
import me.topchetoeu.jscript.engine.scope.LocalScopeRecord;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

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
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        target.add(Instruction.nop(null));

        int start = target.size(), tryN, catchN = -1, finN = -1;

        tryBody.compile(target, scope, false);
        tryN = target.size() - start;

        if (catchBody != null) {
            int tmp = target.size();
            var local = scope instanceof GlobalScope ? scope.child() : (LocalScopeRecord)scope;
            local.define(name, true);
            catchBody.compile(target, scope, false);
            local.undefine();
            catchN = target.size() - tmp;
        }

        if (finallyBody != null) {
            int tmp = target.size();
            finallyBody.compile(target, scope, false);
            finN = target.size() - tmp;
        }

        target.set(start - 1, Instruction.tryInstr(loc(), tryN, catchN, finN));
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
