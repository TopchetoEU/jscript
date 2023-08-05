package me.topchetoeu.jscript.compilation;

import java.util.ArrayList;
import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.control.ContinueStatement;
import me.topchetoeu.jscript.compilation.control.ReturnStatement;
import me.topchetoeu.jscript.compilation.control.ThrowStatement;
import me.topchetoeu.jscript.compilation.values.FunctionStatement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class CompoundStatement extends Statement {
    public final Statement[] statements;

    @Override
    public boolean pollutesStack() {
        for (var stm : statements) {
            if (stm instanceof FunctionStatement) continue;
            return true;
        }

        return false;
    }

    @Override
    public void declare(ScopeRecord varsScope) {
        for (var stm : statements) {
            stm.declare(varsScope);
        }
    }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        for (var stm : statements) {
            if (stm instanceof FunctionStatement) {
                int start = target.size();
                ((FunctionStatement)stm).compile(target, scope, null, true);
                target.get(start).setDebug(true);
                target.add(Instruction.discard());
            }
        }

        for (var i = 0; i < statements.length; i++) {
            var stm = statements[i];
            
            if (stm instanceof FunctionStatement) continue;
            if (i != statements.length - 1) stm.compileNoPollution(target, scope, true);
            else stm.compileWithPollution(target, scope);
        }
    }

    @Override
    public Statement optimize() {
        var res = new ArrayList<Statement>();

        for (var i = 0; i < statements.length; i++) {
            var stm = statements[i].optimize();
            if (i < statements.length - 1 && stm.pure()) continue;
            res.add(stm);
            if (
                stm instanceof ContinueStatement ||
                stm instanceof ReturnStatement ||
                stm instanceof ThrowStatement ||
                stm instanceof ContinueStatement
            ) break;
        }

        if (res.size() == 1) return res.get(0);
        else return new CompoundStatement(loc(), res.toArray(Statement[]::new));
    }

    public CompoundStatement(Location loc, Statement... statements) {
        super(loc);
        this.statements = statements;
    }
}
