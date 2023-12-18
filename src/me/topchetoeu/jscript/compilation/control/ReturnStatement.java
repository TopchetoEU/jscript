package me.topchetoeu.jscript.compilation.control;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class ReturnStatement extends Statement {
    public final Statement value;

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        if (value == null) target.add(Instruction.loadValue(loc(), null));
        else value.compile(target, scope, true);
        target.add(Instruction.ret(loc()));
    }

    public ReturnStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }
}
