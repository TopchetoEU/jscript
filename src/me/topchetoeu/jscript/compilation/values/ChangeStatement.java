package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.AssignableStatement;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.compilation.Instruction.Type;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class ChangeStatement extends Statement {
    public final AssignableStatement value;
    public final double addAmount;
    public final boolean postfix;

    @Override
    public boolean pollutesStack() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        value.toAssign(new ConstantStatement(loc(), -addAmount), Type.SUBTRACT).compileWithPollution(target, scope);
        if (postfix) {
            target.add(Instruction.loadValue(addAmount).locate(loc()));
            target.add(Instruction.operation(Type.SUBTRACT).locate(loc()));
        }
    }

    public ChangeStatement(Location loc, AssignableStatement value, double addAmount, boolean postfix) {
        super(loc);
        this.value = value;
        this.addAmount = addAmount;
        this.postfix = postfix;
    }
}
