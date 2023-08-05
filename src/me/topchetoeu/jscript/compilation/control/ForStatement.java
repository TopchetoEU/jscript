package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.values.ConstantStatement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.engine.values.Values;

public class ForStatement extends Statement {
    public final Statement declaration, assignment, condition, body;
    public final String label;

    @Override
    public boolean pollutesStack() { return false; }

    @Override
    public void declare(ScopeRecord globScope) {
        declaration.declare(globScope);
        body.declare(globScope);
    }
    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        declaration.compile(target, scope);

        if (condition instanceof ConstantStatement) {
            if (Values.toBoolean(((ConstantStatement)condition).value)) {
                int start = target.size();
                body.compileNoPollution(target, scope);
                int mid = target.size();
                assignment.compileNoPollution(target, scope, true);
                int end = target.size();
                WhileStatement.replaceBreaks(target, label, start, mid, mid, end + 1);
                target.add(Instruction.jmp(start - target.size()).locate(loc()));
                return;
            }
        }

        int start = target.size();
        condition.compileWithPollution(target, scope);
        int mid = target.size();
        target.add(Instruction.nop());
        body.compileNoPollution(target, scope);
        int beforeAssign = target.size();
        assignment.compile(target, scope);
        int end = target.size();

        WhileStatement.replaceBreaks(target, label, mid + 1, end, beforeAssign, end + 1);

        target.add(Instruction.jmp(start - end).locate(loc()));
        target.set(mid, Instruction.jmpIfNot(end - mid + 1).locate(loc()));
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
