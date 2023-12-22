package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class DeleteStatement extends Statement {
    public final Statement key;
    public final Statement value;

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        value.compile(target, scope, true);
        key.compile(target, scope, true);

        target.add(Instruction.delete(loc()));
        if (pollute) target.add(Instruction.loadValue(loc(), true));
    }

    public DeleteStatement(Location loc, Statement key, Statement value) {
        super(loc);
        this.key = key;
        this.value = value;
    }
}
