package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class ReturnStatement extends Statement {
    public final Statement value;

    @Override
    public boolean pollutesStack() { return false; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        if (value == null) target.add(Instruction.loadValue(null).locate(loc()));
        else value.compileWithPollution(target, scope);
        target.add(Instruction.ret().locate(loc()));
    }

    public ReturnStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }
}
