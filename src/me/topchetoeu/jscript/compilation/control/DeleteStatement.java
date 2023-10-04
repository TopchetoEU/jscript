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
    public void compile(List<Instruction> target, ScopeRecord scope, boolean pollute) {
        value.compile(target, scope, true);
        key.compile(target, scope, true);

        target.add(Instruction.delete().locate(loc()));
        if (!pollute) target.add(Instruction.discard().locate(loc()));
    }

    public DeleteStatement(Location loc, Statement key, Statement value) {
        super(loc);
        this.key = key;
        this.value = value;
    }
}
