package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.CompoundStatement;
import me.topchetoeu.jscript.compilation.DiscardStatement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.compilation.values.ConstantStatement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.engine.values.Values;

public class WhileStatement extends Statement {
    public final Statement condition, body;
    public final String label;

    @Override
    public void declare(ScopeRecord globScope) {
        body.declare(globScope);
    }
    @Override
    public void compile(List<Instruction> target, ScopeRecord scope, boolean pollute) {
        if (condition instanceof ConstantStatement) {
            if (Values.toBoolean(((ConstantStatement)condition).value)) {
                int start = target.size();
                body.compile(target, scope, false);
                int end = target.size();
                replaceBreaks(target, label, start, end, start, end + 1);
                target.add(Instruction.jmp(start - target.size()).locate(loc()));
                return;
            }
        }

        int start = target.size();
        condition.compile(target, scope, true);
        int mid = target.size();
        target.add(Instruction.nop());
        body.compile(target, scope, false);

        int end = target.size();

        replaceBreaks(target, label, mid + 1, end, start, end + 1);

        target.add(Instruction.jmp(start - end).locate(loc()));
        target.set(mid, Instruction.jmpIfNot(end - mid + 1).locate(loc()));
        if (pollute) target.add(Instruction.loadValue(null).locate(loc()));
    }
    @Override
    public Statement optimize() {
        var cond = condition.optimize();
        var b = body.optimize();

        if (b instanceof ContinueStatement) {
            b = new CompoundStatement(loc());
        }
        else if (b instanceof BreakStatement) return new DiscardStatement(loc(), cond).optimize();

        if (b.pure()) return new WhileStatement(loc(), label, cond, new CompoundStatement(null));
        else return new WhileStatement(loc(), label, cond, b);
    }

    public WhileStatement(Location loc, String label, Statement condition, Statement body) {
        super(loc);
        this.label = label;
        this.condition = condition;
        this.body = body;
    }

    public static void replaceBreaks(List<Instruction> target, String label, int start, int end, int continuePoint, int breakPoint) {
        for (int i = start; i < end; i++) {
            var instr = target.get(i);
            if (instr.type == Type.NOP && instr.is(0, "cont") && (instr.get(1) == null || instr.is(1, label))) {
                target.set(i, Instruction.jmp(continuePoint - i));
                target.get(i).location = instr.location;
            }
            if (instr.type == Type.NOP && instr.is(0, "break") && (instr.get(1) == null || instr.is(1, label))) {
                target.set(i, Instruction.jmp(breakPoint - i));
                target.get(i).location = instr.location;
            }
        }
    }

    // public static CompoundStatement ofFor(Location loc, String label, Statement declaration, Statement condition, Statement increment, Statement body) {
    //     return new CompoundStatement(loc,
    //         declaration,
    //         new WhileStatement(loc, label, condition, new CompoundStatement(loc,
    //             body,
    //             increment
    //         ))
    //     );
    // }
}
