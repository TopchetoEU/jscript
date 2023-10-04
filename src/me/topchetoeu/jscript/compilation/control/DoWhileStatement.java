package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.values.ConstantStatement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.engine.values.Values;

public class DoWhileStatement extends Statement {
    public final Statement condition, body;
    public final String label;

    @Override
    public void declare(ScopeRecord globScope) {
        body.declare(globScope);
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        if (condition instanceof ConstantStatement) {
            int start = target.size();
            body.compile(target, scope, false);
            int end = target.size();
            if (Values.toBoolean(((ConstantStatement)condition).value)) {
                WhileStatement.replaceBreaks(target, label, start, end, end + 1, end + 1);
            }
            else {
                target.add(Instruction.jmp(start - end).locate(loc()));
                WhileStatement.replaceBreaks(target, label, start, end, start, end + 1);
            }
            if (pollute) target.add(Instruction.loadValue(null).locate(loc()));
            return;
        }

        int start = target.size();
        body.compileWithDebug(target, scope, false);
        int mid = target.size();
        condition.compile(target, scope, true);
        int end = target.size();

        WhileStatement.replaceBreaks(target, label, start, mid - 1, mid, end + 1);
        target.add(Instruction.jmpIf(start - end).locate(loc()));
    }

    @Override
    public Statement optimize() {
        var cond = condition.optimize();
        var b = body.optimize();

        if (b instanceof CompoundStatement) {
            var comp = (CompoundStatement)b;
            if (comp.statements.length > 0) {
                var last = comp.statements[comp.statements.length - 1];
                if (last instanceof ContinueStatement) comp.statements[comp.statements.length - 1] = new CompoundStatement(loc());
                if (last instanceof BreakStatement) {
                    comp.statements[comp.statements.length - 1] = new CompoundStatement(loc());
                    return new CompoundStatement(loc());
                }
            }
        }
        else if (b instanceof ContinueStatement) {
            b = new CompoundStatement(loc());
        }
        else if (b instanceof BreakStatement) return new CompoundStatement(loc());

        if (b.pure()) return new DoWhileStatement(loc(), label, cond, new CompoundStatement(loc()));
        else return new DoWhileStatement(loc(), label, cond, b);
    }

    public DoWhileStatement(Location loc, String label, Statement condition, Statement body) {
        super(loc);
        this.label = label;
        this.condition = condition;
        this.body = body;
    }
}
