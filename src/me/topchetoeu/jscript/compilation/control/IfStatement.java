package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.DiscardStatement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.values.ConstantStatement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.engine.values.Values;

public class IfStatement extends Statement {
    public final Statement condition, body, elseBody;

    @Override
    public boolean pollutesStack() { return false; }

    @Override
    public void declare(ScopeRecord globScope) {
        body.declare(globScope);
        if (elseBody != null) elseBody.declare(globScope);
    }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        if (condition instanceof ConstantStatement) {
            if (Values.not(((ConstantStatement)condition).value)) {
                if (elseBody != null) elseBody.compileNoPollution(target, scope, true);
            }
            else {
                body.compileNoPollution(target, scope, true);
            }

            return;
        }

        condition.compileWithPollution(target, scope);
        if (elseBody == null) {
            int i = target.size();
            target.add(Instruction.nop());
            body.compileNoPollution(target, scope, true);
            int endI = target.size();
            target.set(i, Instruction.jmpIfNot(endI - i).locate(loc()));
        }
        else {
            int start = target.size();
            target.add(Instruction.nop());
            body.compileNoPollution(target, scope, true);
            target.add(Instruction.nop());
            int mid = target.size();
            elseBody.compileNoPollution(target, scope, true);
            int end = target.size();

            target.set(start, Instruction.jmpIfNot(mid - start).locate(loc()));
            target.set(mid - 1, Instruction.jmp(end - mid + 1).locate(loc()));
        }
    }
    
    @Override
    public Statement optimize() {
        var cond = condition.optimize();
        var b = body.optimize();
        var e = elseBody == null ? null : elseBody.optimize();

        if (b.pure()) b = new CompoundStatement(null);
        if (e != null && e.pure()) e = null;

        if (b.pure() && e == null) return new DiscardStatement(loc(), cond).optimize();
        else return new IfStatement(loc(), cond, b, e);
    }

    public IfStatement(Location loc, Statement condition, Statement body, Statement elseBody) {
        super(loc);
        this.condition = condition;
        this.body = body;
        this.elseBody = elseBody;
    }
}
