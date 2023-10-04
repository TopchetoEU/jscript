package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class ArrayStatement extends Statement {
    public final Statement[] statements;

    @Override
    public boolean pure() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope, boolean pollute) {
        target.add(Instruction.loadArr(statements.length).locate(loc()));
        var i = 0;
        for (var el : statements) {
            if (el != null) {
                target.add(Instruction.dup().locate(loc()));
                target.add(Instruction.loadValue(i).locate(loc()));
                el.compile(target, scope, true);
                target.add(Instruction.storeMember().locate(loc()));
            }
            i++;
        }
        if (!pollute) target.add(Instruction.discard().locate(loc()));
    }

    public ArrayStatement(Location loc, Statement[] statements) {
        super(loc);
        this.statements = statements;
    }
}
