package me.topchetoeu.jscript.compilation.values;

import java.util.Vector;

import me.topchetoeu.jscript.Location;
import me.topchetoeu.jscript.compilation.CompileTarget;
import me.topchetoeu.jscript.compilation.Statement;
import me.topchetoeu.jscript.engine.scope.ScopeRecord;

public class CommaStatement extends Statement {
    public final Statement[] values;

    @Override
    public void compile(CompileTarget target, ScopeRecord scope, boolean pollute) {
        for (var i = 0; i < values.length; i++) {
            values[i].compileWithDebug(target, scope, i == values.length - 1 && pollute);
        }
    }
    
    @Override
    public Statement optimize() {
        var res = new Vector<Statement>(values.length);

        for (var i = 0; i < values.length; i++) {
            var stm = values[i].optimize();
            if (i < values.length - 1 && stm.pure()) continue;
            res.add(stm);
        }

        if (res.size() == 1) return res.get(0);
        else return new CommaStatement(loc(), res.toArray(Statement[]::new));
    }

    public CommaStatement(Location loc, Statement ...args) {
        super(loc);
        this.values = args;
    }
}
