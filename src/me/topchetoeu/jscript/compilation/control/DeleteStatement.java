package me.topchetoeu.jscript.compilation.control;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class DeleteStatement extends Statement {
    public final Statement key;
    public final Statement value;

    @Override
    public boolean pollutesStack() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        value.compile(target, scope);
        key.compile(target, scope);
        target.add(Instruction.delete().locate(loc()));
    }

    public DeleteStatement(Location loc, Statement key, Statement value) {
        super(loc);
        this.key = key;
        this.value = value;
    }
}
