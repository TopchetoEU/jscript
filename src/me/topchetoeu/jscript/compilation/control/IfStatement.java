package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
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
    public void declare(ScopeRecord globScope) {
        body.declare(globScope);
        if (elseBody != null) elseBody.declare(globScope);
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        if (condition instanceof ConstantStatement) {
            if (Values.not(((ConstantStatement)condition).value)) {
                if (elseBody != null) elseBody.compileWithDebug(target, scope, pollute);
            }
            else {
                body.compileWithDebug(target, scope, pollute);
            }

            return;
        }

        condition.compile(target, scope, true);

        if (elseBody == null) {
            int i = target.size();
            target.add(Instruction.nop(null));
            body.compileWithDebug(target, scope, pollute);
            int endI = target.size();
            target.set(i, Instruction.jmpIfNot(loc(), endI - i));
        }
        else {
            int start = target.size();
            target.add(Instruction.nop(null));
            body.compileWithDebug(target, scope, pollute);
            target.add(Instruction.nop(null));
            int mid = target.size();
            elseBody.compileWithDebug(target, scope, pollute);
            int end = target.size();

            target.set(start, Instruction.jmpIfNot(loc(), mid - start));
            target.set(mid - 1, Instruction.jmp(loc(), end - mid + 1));
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
