package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.common.Location;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.CompileResult;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.core.Operation;

public class ChangeStatement extends Statement {
    public final AssignableStatement value;
    public final double addAmount;
    public final boolean postfix;

    @Override
    public void compile(CompileResult target, boolean pollute) {
        value.toAssign(new ConstantStatement(loc(), -addAmount), Operation.SUBTRACT).compile(target, true);
        if (!pollute) target.add(Instruction.discard());
        else if (postfix) {
            target.add(Instruction.pushValue(addAmount));
            target.add(Instruction.operation(Operation.SUBTRACT));
        }
    }

    public ChangeStatement(Location loc, AssignableStatement value, double addAmount, boolean postfix) {
        super(loc);
        this.value = value;
        this.addAmount = addAmount;
        this.postfix = postfix;
    }
}
