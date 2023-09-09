package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.Location;
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
    public boolean pollutesStack() { return false; }

    @Override
    public void declare(ScopeRecord globScope) {
        tryBody.declare(globScope);
        if (catchBody != null) catchBody.declare(globScope);
        if (finallyBody != null) finallyBody.declare(globScope);
    }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        target.add(Instruction.nop());

        int start = target.size(), tryN, catchN = -1, finN = -1;

        tryBody.compileNoPollution(target, scope);
        tryN = target.size() - start;

        if (catchBody != null) {
            int tmp = target.size();
            var local = scope instanceof GlobalScope ? scope.child() : (LocalScopeRecord)scope;
            local.define(name, true);
            catchBody.compileNoPollution(target, scope);
            local.undefine();
            catchN = target.size() - tmp;
        }

        if (finallyBody != null) {
            int tmp = target.size();
            finallyBody.compileNoPollution(target, scope);
            finN = target.size() - tmp;
        }

        target.set(start - 1, Instruction.tryInstr(tryN, catchN, finN).locate(loc()));
    }

    public TryStatement(Location loc, Statement tryBody, Statement catchBody, Statement finallyBody, String name) {
        super(loc);
        this.tryBody = tryBody;
        this.catchBody = catchBody;
        this.finallyBody = finallyBody;
        this.name = name;
    }
}
