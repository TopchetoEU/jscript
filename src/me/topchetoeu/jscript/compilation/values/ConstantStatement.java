package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class ConstantStatement extends Statement {
    public final Object value;

    @Override
    public boolean pollutesStack() { return true; }
    @Override
    public boolean pure() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        target.add(Instruction.loadValue(value).locate(loc()));
    }

    public ConstantStatement(Location loc, Object val) {
        super(loc);
        this.value = val;
    }
}
