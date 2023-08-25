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
            local.define(name);
            catchBody.compileNoPollution(target, scope);
            local.undefine();
            catchN = target.size() - tmp;
        }

        if (finallyBody != null) {
            int tmp = target.size();
            finallyBody.compileNoPollution(target, scope);
            finN = target.size() - tmp;
        }

        // for (int i = start; i < target.size(); i++) {
        //     if (target.get(i).type == Type.NOP) {
        //         var instr = target.get(i);
        //         if (instr.is(0, "break")) {
        //             target.set(i, Instruction.nop("try_break", instr.get(1), target.size()).locate(instr.location));
        //         }
        //         else if (instr.is(0, "cont")) {
        //             target.set(i, Instruction.nop("try_cont", instr.get(1), target.size()).locate(instr.location));
        //         }
        //     }
        // }

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
