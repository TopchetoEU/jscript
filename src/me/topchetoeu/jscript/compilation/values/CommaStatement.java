package me.topchetoeu.jscript.compilation.values;

import java.util.List;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.Instruction;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class CommaStatement extends Statement {
    public final Statement first;
    public final Statement second;

    @Override
    public boolean pollutesStack() { return true; }
    @Override
    public boolean pure() { return first.pure() && second.pure(); }

    @Override
    public void compile(List<Instruction> target, ScopeRecord scope) {
        first.compileNoPollution(target, scope);
        second.compileWithPollution(target, scope);
    }

    @Override
    public Statement optimize() {
        var f = first.optimize();
        var s = second.optimize();
        if (f.pure()) return s;
        else return new CommaStatement(loc(), f, s);
    }

    public CommaStatement(Location loc, Statement first, Statement second) {
        super(loc);
        this.first = first;
        this.second = second;
    }
}
