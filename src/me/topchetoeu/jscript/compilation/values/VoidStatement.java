package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;
import me.topchetoeu.jscript.compilation.Instruction;

public class VoidStatement extends Statement {
    public final Statement value;

    @Override
    public boolean pollutesStack() { return true; }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        if (value != null) value.compileNoPollution(target, scope);
        target.add(Instruction.loadValue(null).locate(loc()));
    }

    @Override
    public Statement optimize() {
        if (value == null) return this;
        var val = value.optimize();
        if (val.pure()) return new ConstantStatement(loc(), null);
        else return new VoidStatement(loc(), val);
    }

    public VoidStatement(Location loc, Statement value) {
        super(loc);
        this.value = value;
    }
}
