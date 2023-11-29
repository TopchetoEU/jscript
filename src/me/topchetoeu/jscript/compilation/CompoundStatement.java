package me.topchetoeu.jscript.compilation;

import java.util.Vector;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.control.ContinueStatement;
import me.topchetoeu.jscript.compilation.control.ReturnStatement;
import me.topchetoeu.jscript.compilation.control.ThrowStatement;
import me.topchetoeu.jscript.compilation.values.FunctionStatement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class CompoundStatement extends Statement {
    public final Statement[] statements;
    public Location end;

    @Override
    public void declare(ScopeRecord varsScope) {
        for (var stm : statements) {
            stm.declare(varsScope);
        }
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        for (var stm : statements) {
            if (stm instanceof FunctionStatement) stm.compile(target, scope, false);
        }

        for (var i = 0; i < statements.length; i++) {
            var stm = statements[i];

            if (stm instanceof FunctionStatement) continue;
            if (i != statements.length - 1) stm.compileWithDebug(target, scope, false);
            else stm.compileWithDebug(target, scope, pollute);
        }

        if (end != null) {
            target.add(Instruction.nop(end));
            target.setDebug();
        }
    }

    @Override
    public Statement optimize() {
        var res = new Vector<Statement>(statements.length);

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

    public CompoundStatement setEnd(Location loc) {
        this.end = loc;
        return this;
    }

    public CompoundStatement(Location loc, Statement ...statements) {
        super(loc);
        this.statements = statements;
    }
}
