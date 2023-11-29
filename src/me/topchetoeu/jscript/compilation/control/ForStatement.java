package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.values.ConstantStatement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.engine.values.Values;

public class ForStatement extends Statement {
    public final Statement declaration, assignment, condition, body;
    public final String label;

    @Override
    public void declare(ScopeRecord globScope) {
        declaration.declare(globScope);
        body.declare(globScope);
    }
    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        declaration.compile(target, scope, false);

        if (condition instanceof ConstantStatement) {
            if (Values.toBoolean(((ConstantStatement)condition).value)) {
                int start = target.size();
                body.compile(target, scope, false);
                int mid = target.size();
                assignment.compileWithDebug(target, scope, false);
                int end = target.size();
                WhileStatement.replaceBreaks(target, label, start, mid, mid, end + 1);
                target.add(Instruction.jmp(loc(), start - target.size()));
                if (pollute) target.add(Instruction.loadValue(loc(), null));
            }
            return;
        }

        int start = target.size();
        condition.compile(target, scope, true);
        int mid = target.size();
        target.add(Instruction.nop(null));
        body.compile(target, scope, false);
        int beforeAssign = target.size();
        assignment.compileWithDebug(target, scope, false);
        int end = target.size();

        WhileStatement.replaceBreaks(target, label, mid + 1, end, beforeAssign, end + 1);

        target.add(Instruction.jmp(loc(), start - end));
        target.set(mid, Instruction.jmpIfNot(loc(), end - mid + 1));
        if (pollute) target.add(Instruction.loadValue(loc(), null));
    }
    @Override
    public Statement optimize() {
        var decl = declaration.optimize();
        var asgn = assignment.optimize();
        var cond = condition.optimize();
        var b = body.optimize();

        if (asgn.pure()) {
            if (decl.pure()) return new WhileStatement(loc(), label, cond, b).optimize();
            else return new CompoundStatement(loc(),
                decl, new WhileStatement(loc(), label, cond, b)
            ).optimize();
        }

        else if (b instanceof ContinueStatement) return new CompoundStatement(loc(),
            decl, new WhileStatement(loc(), label, cond, new CompoundStatement(loc(), b, asgn))
        );
        else if (b instanceof BreakStatement) return decl;

        if (b.pure()) return new ForStatement(loc(), label, decl, cond, asgn, new CompoundStatement(null));
        else return new ForStatement(loc(), label, decl, cond, asgn, b);
    }

    public ForStatement(Location loc, String label, Statement declaration, Statement condition, Statement assignment, Statement body) {
        super(loc);
        this.label = label;
        this.declaration = declaration;
        this.condition = condition;
        this.assignment = assignment;
        this.body = body;
    }

    public static CompoundStatement ofFor(Location loc, String label, Statement declaration, Statement condition, Statement increment, Statement body) {
        return new CompoundStatement(loc,
            declaration,
            new WhileStatement(loc, label, condition, new CompoundStatement(loc,
                body,
                increment
            ))
        );
    }
}
