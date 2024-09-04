package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Instruction;
import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Statement;

public class ArrayStatement extends Statement {
    public final Statement[] statements;

    @Override public boolean pure() {
        for (var stm : statements) {
            if (!stm.pure()) return false;
        }

        return true;
    }

    @Override
    public void compile(CompileResult target, boolean pollute) {
        target.add(Instruction.loadArr(statements.length));

        for (var i = 0; i < statements.length; i++) {
            var el = statements[i];
            if (el != null) {
                target.add(Instruction.dup());
                target.add(Instruction.pushValue(i));
                el.compile(target, true);
                target.add(Instruction.storeMember());
            }
        }

        if (!pollute) target.add(Instruction.discard());
    }

    public ArrayStatement(Location loc, Statement[] statements) {
        super(loc);
        this.statements = statements;
    }
}
