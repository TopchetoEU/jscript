package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class ArrayStatement extends Statement {
    public final Statement[] statements;

    @Override public boolean pure() {
        for (var stm : statements) {
            if (!stm.pure()) return false;
        }

        return true;
    }

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        target.add(Instruction.loadArr(loc(), statements.length));

        for (var i = 0; i < statements.length; i++) {
            var el = statements[i];
            if (el != null) {
                target.add(Instruction.dup(loc()));
                target.add(Instruction.loadValue(loc(), i));
                el.compile(target, scope, true);
                target.add(Instruction.storeMember(loc()));
            }
        }

        if (!pollute) target.add(Instruction.discard(loc()));
    }

    public ArrayStatement(Location loc, Statement[] statements) {
        super(loc);
        this.statements = statements;
    }
}
