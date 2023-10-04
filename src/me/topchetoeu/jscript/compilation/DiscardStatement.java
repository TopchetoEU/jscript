package me.topchetoeu.jscript.compilation;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.values.ConstantStatement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class DiscardStatement extends Statement {
    public final Statement value;

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope, boolean pollute) {
        value.compile(target, scope, false);
        
    }
    @Override
    public Statement optimize() {
        if (value == null) return this;
        var val = value.optimize();
        if (val.pure()) return new ConstantStatement(loc(), null);
        else return new DiscardStatement(loc(), val);
    }

    public DiscardStatement(Location loc, Statement val) {
        super(loc);
        this.value = val;
    }
}
