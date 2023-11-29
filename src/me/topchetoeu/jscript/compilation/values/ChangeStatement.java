package me.topchetoeu.jscript.compilation.values;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.Operation;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class ChangeStatement extends Statement {
    public final AssignableStatement value;
    public final double addAmount;
    public final boolean postfix;

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        value.toAssign(new ConstantStatement(loc(), -addAmount), Operation.SUBTRACT).compile(target, scope, true);
        if (!pollute) target.add(Instruction.discard(loc()));
        else if (postfix) {
            target.add(Instruction.loadValue(loc(), addAmount));
            target.add(Instruction.operation(loc(), Operation.SUBTRACT));
        }
    }

    public ChangeStatement(Location loc, AssignableStatement value, double addAmount, boolean postfix) {
        super(loc);
        this.value = value;
        this.addAmount = addAmount;
        this.postfix = postfix;
    }
}
